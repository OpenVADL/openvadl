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

package vadl.iss.passes.common.opDecomposition;

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
import vadl.iss.passes.common.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.iss.passes.tcg.lowering.Tcg_32_64;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.DataType;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.Procedure;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

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
        .flatMap(alias -> Stream.of(
            alias.readFunction().behavior(),
            alias.writeProcedure().behavior()
        ))
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
    // decompose nodes until there any more nodes to decompose.
    var foundOne = true;
    var processed = new HashSet<Node>();
    int iteration = 0;
    while (foundOne) {
      // first decompose side effects, then expressions.
      iteration++;
      var oversizedBefore = oversizedSideEffectCount();
      var sideEffectHit = decomposeSideEffects();
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
    // Remove them so later base-accessor descriptor retrieval only sees effective accesses.
    behavior.deleteUnusedDependencies();
  }

  private boolean decomposeSideEffects() {
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

}
