// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss.passes.opDecomposition;

import static vadl.types.BuiltInTable.LSL;
import static vadl.types.BuiltInTable.MUL;
import static vadl.types.BuiltInTable.OR;
import static vadl.types.BuiltInTable.SMULL;
import static vadl.types.BuiltInTable.SUMULL;
import static vadl.types.BuiltInTable.UMULL;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.IssConfiguration;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.iss.passes.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.opDecomposition.nodes.IssMulKind;
import vadl.iss.passes.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.ViamUtils;
import vadl.viam.Constant;
import vadl.viam.Instruction;
import vadl.viam.Procedure;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * This pass splits certain operations in the behavior into multiple nodes, depending on the
 * context.
 * Most of the new nodes are ISS intermediate nodes, such as {@link IssMulhNode}.
 * For example, if a result of an operation node exceeds the target size, we cannot
 * handle it in QEMU, so we must split the operation into multiple smaller sized ones.
 * If the context allows it, we might also replace it by a non-equivalent alternative node.
 * E.g. if there is a long multiplication (such as {@code SMULL}) and the result is only used
 * by a slice or truncate that takes the upper or lower half, we can directly replace it
 * by a {@link IssMulhNode} or a normal {@code MUL} built-in call.
 *
 * <p>From paper: While the VADL specification allows arbitrary bit widths,
 * QEMU imposes a 64-bit limit for most operations. This becomes problematic when an instruction
 * specification requires types larger than 64 bits. For example, the MULH instruction in the
 * RV64IM specification performs a long multiplication of two 64-bit values and extracts the
 * upper half of the 128-bit result.
 * To handle such cases, the Operation Decomposition pass splits these operations into multiple
 * logically equivalent operations that only accept and return values with a maximum
 * size of 64 bits.</p>
 */
public class IssOpDecompositionPass extends AbstractIssPass {
  private static final Logger LOG = LoggerFactory.getLogger(IssOpDecompositionPass.class);

  public IssOpDecompositionPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Op Decomposition Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var largeOperationErrors = new ArrayList<Diagnostic>();
    var aliasSemanticBehaviors = viam.artificialResources()
        .flatMap(alias -> Stream.of(alias.readFunction().behavior(), alias.writeProcedure().behavior()))
        .collect(() -> java.util.Collections.newSetFromMap(new IdentityHashMap<>()),
            Set::add,
            Set::addAll);
    ViamUtils.findAllBehaviors(viam)
        .filter(behavior -> !aliasSemanticBehaviors.contains(behavior))
        .filter(behavior -> behavior.parentDefinition() instanceof Instruction
            || behavior.parentDefinition() instanceof Procedure)
        .forEach(behavior -> {
          var behaviorName = behavior.toString();
          var startNs = System.nanoTime();
          LOG.debug("OpDecompose start: {}", behaviorName);
          new OpDecomposer(behavior, configuration().targetSize(), behaviorName).decompose();
          var durMs = (System.nanoTime() - startNs) / 1_000_000;
          LOG.debug("OpDecompose done: {} ({} ms)", behaviorName, durMs);

          checkNoLargeOperations(behavior).ifPresent(largeOperationErrors::add);
        });

    if (!largeOperationErrors.isEmpty()) {
      throw new DiagnosticList(largeOperationErrors);
    }

    return null;
  }

  private Optional<Diagnostic> checkNoLargeOperations(Graph behavior) {
    var tooLargeExpr = behavior.getNodes(ExpressionNode.class)
        .filter(n -> n.type() instanceof DataType)
        .filter(n -> expressionBitWidth(n) > configuration().targetSize().width)
        .map(n -> Diagnostic.error("Too large operation type", n)
            .locationDescription(n, "The ISS was not able to decompose this to smaller types.")
            .note("Operation decomposition is still in early stages and "
                + "only implemented for special cases.")
            .build())
        .findFirst();
    if (tooLargeExpr.isPresent()) {
      return tooLargeExpr;
    }

    var tooLargeMemWrite = behavior.getNodes(WriteMemNode.class)
        .filter(w -> w.writeBitWidth() > configuration().targetSize().width)
        .map(w -> Diagnostic.error("Too large memory write", w)
            .locationDescription(w,
                "The ISS was not able to decompose this write to target-sized chunks.")
            .build())
        .findFirst();
    if (tooLargeMemWrite.isPresent()) {
      return tooLargeMemWrite;
    }

    return behavior.getNodes(WriteRegTensorNode.class)
        .filter(w -> w.writeBitWidth() > configuration().targetSize().width)
        .map(w -> Diagnostic.error("Too large register write", w)
            .locationDescription(w,
                "The ISS was not able to decompose this write to target-sized chunks.")
            .build())
        .findFirst();
  }

  private int expressionBitWidth(ExpressionNode expr) {
    if (expr instanceof IssReadRegNode ir) {
      return ir.readBitWidth();
    }
    return expr.type().asDataType().bitWidth();
  }
}

/**
 * This is the old decomposer, which is highly specialized on long mul.
 * We should move the implementation to the {@link Decomposer} which can handle
 * things more generalized. However, it might be necessary to find certain patterns for
 * performance reasons.
 */
class OpDecomposer {
  private static final Logger LOG = LoggerFactory.getLogger(OpDecomposer.class);

  Tcg_32_64 targetSize;
  Graph behavior;
  String instrName;
  int exprDecomposeCount = 0;

  public OpDecomposer(Graph behavior, Tcg_32_64 targetSize, String instrName) {
    this.behavior = behavior;
    this.targetSize = targetSize;
    this.instrName = instrName;
  }

  void decompose() {
    behavior.getNodes(BuiltInCall.class).forEach(this::handle);
    // delete all dependency nodes that are not used anymore
    behavior.deleteUnusedDependencies();

    // decompose nodes until there any more nodes to decompose.
    var foundOne = true;
    var processed = new HashSet<Node>();
    int iteration = 0;
    while (foundOne) {
      // first decompose side effects, then expressions.
      iteration++;
      var oversizedBefore = oversizedSideEffectCount();
      var sideEffectHit = decomposeSideeffects();
      if (sideEffectHit) {
        var oversizedAfter = oversizedSideEffectCount();
        behavior.ensure(oversizedAfter < oversizedBefore,
            "Side-effect decomposition did not make progress in %s: oversized writes before=%d "
                + "after=%d",
            instrName, oversizedBefore, oversizedAfter);
      }
      if (sideEffectHit) {
        LOG.debug("OpDecompose [{}] iteration {}: decomposed side effect", instrName, iteration);
      }
      var exprHit = sideEffectHit ? false : decomposeExpressions(processed);
      if (exprHit) {
        LOG.debug("OpDecompose [{}] iteration {}: decomposed expression, processed={}", instrName,
            iteration, processed.size());
      }
      foundOne = sideEffectHit || exprHit;
      if (!foundOne) {
        LOG.debug("OpDecompose [{}] converged after {} iterations", instrName, iteration);
      }
    }

    // Decomposition may leave replaced wide read nodes dangling.
    // Remove them so later access-pattern retrieval only sees effective accesses.
    behavior.deleteUnusedDependencies();
  }

  private boolean decomposeSideeffects() {
    var hit = Stream.concat(
            behavior.getNodes(WriteMemNode.class).map(SideEffectNode.class::cast),
            behavior.getNodes(IssWriteRegNode.class).map(SideEffectNode.class::cast)
        )
        .filter(node -> !node.isDeleted())
        .filter(node -> {
          if (node instanceof WriteMemNode w) {
            return w.writeBitWidth() > targetSize.width;
          }
          if (node instanceof IssWriteRegNode w) {
            return w.writeBitWidth() > targetSize.width;
          }
          return false;
        })
        .findFirst();
    if (hit.isPresent()) {
      new Decomposer(targetSize.width).decompose(hit.get());
      return true;
    }
    return false;
  }

  private long oversizedSideEffectCount() {
    return behavior.getNodes(SideEffectNode.class)
        .filter(node -> !node.isDeleted())
        .filter(node -> {
          if (node instanceof WriteMemNode w) {
            return w.writeBitWidth() > targetSize.width;
          }
          if (node instanceof IssWriteRegNode w) {
            return w.writeBitWidth() > targetSize.width;
          }
          return false;
        })
        .count();
  }

  private boolean decomposeExpressions(Set<Node> processed) {
    Deque<ExpressionNode> worklist = new ArrayDeque<>();
    behavior.getNodes(ExpressionNode.class)
        .filter(this::isDecomposeExpressionCandidate)
        .filter(node -> !processed.contains(node))
        .forEach(worklist::addLast);

    while (!worklist.isEmpty()) {
      var node = worklist.removeFirst();
      if (node.isDeleted() || processed.contains(node) || !isDecomposeExpressionCandidate(node)) {
        continue;
      }

      exprDecomposeCount++;
      if (LOG.isDebugEnabled() && (exprDecomposeCount <= 20 || exprDecomposeCount % 500 == 0)) {
        var kind = node.getClass().getSimpleName();
        var detail = node instanceof BuiltInCall b
            ? b.builtIn().toString()
            : kind;
        var inputW = node.inputs().map(ExpressionNode.class::cast)
            .map(i -> Integer.toString(i.type().asDataType().bitWidth()))
            .reduce((a, b) -> a + "," + b)
            .orElse("-");
        LOG.debug("OpDecompose [{}] hit #{}: {} inputWidths=[{}] outWidth={}",
            instrName, exprDecomposeCount, detail, inputW, node.type().asDataType().bitWidth());
      }

      new Decomposer(targetSize.width).decompose(node);
      processed.add(node);
      return true;
    }

    return false;
  }

  private boolean isDecomposeExpressionCandidate(ExpressionNode node) {
    return node.type() instanceof DataType
        // find any expression node that is within the target size while having a too large input.
        && node.type().asDataType().bitWidth() <= targetSize.width
        && node.inputs().map(ExpressionNode.class::cast)
        .anyMatch(i -> i.type().asDataType().bitWidth() > targetSize.width);
  }

  private void handle(BuiltInCall call) {
    var b = call.builtIn();
    if (b == UMULL || b == SMULL || b == SUMULL) {
      replaceLongMul(call);
    }
  }

  // Handle decomposition of umull and smull call.
  // This will replace it by a call to mul2 which returns two values (upper and lower bits).
  private void replaceLongMul(BuiltInCall call) {
    if (call.type().asDataType().bitWidth() <= targetSize.width) {
      return;
    }

    var kind = call.builtIn() == SUMULL
        ? IssMulKind.SIGNED_UNSIGNED
        : call.builtIn() == SMULL
          ? IssMulKind.SIGNED_SIGNED
          : IssMulKind.UNSIGNED_UNSIGNED;

    for (var user : call.usages().toList()) {
      replaceLongMulForUser(call, user, kind);
    }
  }

  private void replaceLongMulForUser(BuiltInCall longMul, Node user, IssMulKind kind) {
    // check that user is slice or truncate.
    // otherwise, we currently cannot handle this built-in call, as
    // the value is too big to hold it in a QEMU TCGv.
    checkUserSliceOrTruncate(longMul, user);

    var arg1 = longMul.arguments().get(0);
    var argWidth = arg1.type().asDataType().bitWidth();
    var singleLengthType = Type.bits(argWidth);

    if (user instanceof TruncateNode) {
      // if the user is just a truncate node, we can also use the normal MUL built-in instead.
      user.replaceInput(longMul,
          new BuiltInCall(MUL, longMul.arguments(), singleLengthType)
      );
      return;
    }

    if (user instanceof SliceNode sliceNode) {
      // currently we only handle continuous slices
      sliceNode.ensure(sliceNode.bitSlice().isContinuous(),
          "Non continuous slices are currently not supported");
      var slice = sliceNode.bitSlice();

      var targetWith = targetSize.width;

      if (slice.msb() < targetWith || slice.lsb() >= targetWith) {
        // the slice does not use parts from both, the upper half and lower half.
        handleSliceWithinUpperOrLowerBoundary(longMul, sliceNode, kind);
      } else {
        // the slice crosses the middle point, so we have to compute upper and lower halves.
        handleSliceAcrossUpperLowerBoundary(longMul, sliceNode, kind);
      }
    }
  }

  private void handleSliceWithinUpperOrLowerBoundary(BuiltInCall longMul, SliceNode sliceNode,
                                                     IssMulKind kind) {
    var arg1 = longMul.arguments().get(0);
    var arg2 = longMul.arguments().get(1);
    var argWidth = arg1.type().asDataType().bitWidth();
    var singleLengthType = Type.bits(argWidth);

    var slice = sliceNode.bitSlice();
    var targetWith = targetSize.width;
    var upperHalf = slice.msb() >= targetWith;

    ExpressionNode longMulReplacement;
    if (upperHalf) {
      // if upper half, we use the mulh operation.
      longMulReplacement = behavior.add(new IssMulhNode(arg1, arg2, kind, singleLengthType));

      // adjust the slice by targetWidth bit, as we are now handling the upper half only.
      sliceNode.setSlice(
          new Constant.BitSlice(
              new Constant.BitSlice.Part(slice.msb() - targetWith, slice.lsb() - targetWith))
      );
    } else {
      // if lower half, we just use the normal mul built-in.
      longMulReplacement =
          behavior.add(new BuiltInCall(MUL, longMul.arguments(), singleLengthType));
    }

    sliceNode.replaceInput(longMul, longMulReplacement);

    // TODO: Refactor this in `TransformerNode` or something similar
    var bitSlice = sliceNode.bitSlice();
    if (bitSlice.lsb() == 0 && bitSlice.bitSize() == argWidth) {
      // the slice is not necessary, we can just remove it
      for (var u : sliceNode.usages().toList()) {
        u.replaceInput(sliceNode, longMulReplacement);
      }
    }
  }

  private void handleSliceAcrossUpperLowerBoundary(BuiltInCall longMul, SliceNode sliceNode,
                                                   IssMulKind kind) {
    var arg1 = longMul.arguments().get(0);
    var arg2 = longMul.arguments().get(1);
    var argWidth = arg1.type().asDataType().bitWidth();

    var targetType = Type.bits(argWidth);
    var tupleType = Type.tuple(targetType, targetType);

    var slice = sliceNode.bitSlice();

    var mul2 = behavior.add(new IssMul2Node(arg1, arg2, kind, tupleType));
    // lower and upper half in target type size (not final expected size yet)
    var lowerHalf = behavior.add(new TupleGetFieldNode(0, mul2, targetType));
    var upperHalf = behavior.add(new TupleGetFieldNode(1, mul2, targetType));

    // lower half sub slice [targetSize - 1 ... lsb]
    var lhMsb = targetSize.width - 1;
    var lhLsb = slice.lsb();
    // +1 because msb and lsb are inclusive
    var lhSize = lhMsb - lhLsb + 1;

    // upper half sub slice [msb - targetSize ... 0]
    var uhMsb = slice.msb() - targetSize.width;
    var uhLsb = 0;
    var uhSize = uhMsb - uhLsb + 1;

    // final size is uhSize + lhSize
    var finalSize = uhSize + lhSize;
    var finalType = Type.bits(finalSize);

    var lowerHalfSlice = behavior.addWithInputs(new ZeroExtendNode(
        new SliceNode(
            lowerHalf,
            new Constant.BitSlice(new Constant.BitSlice.Part(lhMsb, lhLsb)),
            Type.bits(lhSize)
        ), finalType));

    var upperHalfSlice = behavior.addWithInputs(
        new ZeroExtendNode(
            new SliceNode(
                upperHalf,
                new Constant.BitSlice(new Constant.BitSlice.Part(uhMsb, uhLsb)),
                Type.bits(uhSize)
            ), finalType));

    // now we shift the upper half to the correct position upperHalfSlice << lhSize.
    var shiftAmount = new ConstantNode(Constant.Value.of(lhSize, Type.bits(16)));
    var upperHalfShifted = behavior.addWithInputs(new BuiltInCall(
        LSL, new NodeList<>(upperHalfSlice, shiftAmount), finalType
    ));

    // now we merge both halves into a single value
    var combined = behavior.add(new BuiltInCall(
        OR, new NodeList<>(upperHalfShifted, lowerHalfSlice),
        finalType
    ));

    // replace the slice by the new decomposed value.
    sliceNode.replace(combined);
  }


  private void checkUserSliceOrTruncate(BuiltInCall call, Node user) {
    var callLoc = call.location();
    var userLoc = user.location().orDefault(behavior.sourceLocation());
    if (!(user instanceof SliceNode || user instanceof TruncateNode)) {
      throw Diagnostic.error("Slice or cast required", userLoc)
          .description(
              "The ISS currently requires that a smull result greater than %s bit is "
                  + "directly cast or sliced to a value <= %s bit before further usage.",
              targetSize.width, targetSize.width)
          .locationNote(callLoc, "The result of this is %s bits wide.",
              call.type().asDataType().bitWidth())
          .help("Cast the result to something <= %s bit.", targetSize.width)
          .build();
    }

    if (user instanceof TruncateNode truncNode && truncNode.type().bitWidth() > targetSize.width) {
      throw Diagnostic.error("Type to big", userLoc)
          .description("The ISS currently requires a type <= %s bit.", targetSize.width)
          .build();
    }
  }


}
