// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes;

import com.google.errorprone.annotations.FormatMethod;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssExtractNode;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.opDecomposition.nodes.IssMulKind;
import vadl.iss.passes.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.utils.VadlBuiltInNoStatusDispatcher;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadStageOutputNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * QEMU operates on 64 or 32-bit architectures.
 * This means that all variables and operations in the
 * TCG are either 32 bits or 64 bits wide.
 * However, the VADL specification might contain values that are smaller than 64/32 bit.
 * If we just use arithmetic operations on 64 bit with those values, the results were
 * incorrect.
 * Therefore, operations (mostly arithmetic ones) like ADD must possibly truncate
 * their results or sign extend their arguments.
 *
 * <p>This pass inserts {@link IssExtractNode}s where required and replaces all
 * zero/sign extends and truncates by such node.
 * A follow-up pass merges the extract nodes.
 *
 * @see IssExtractNode
 * @see IssExtractOptimizationPass
 */
public class IssNormalizationPass extends Pass {

  public IssNormalizationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Normalization Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var issConfig = (IssConfiguration) configuration();

    viam.isa().get().ownInstructions()
        .forEach(instruction -> new IssNormalizer(instruction.behavior(),
            issConfig.targetSize().width)
            .run());
    return null;
  }
}


@DispatchFor(
    value = ExpressionNode.class,
    include = {"vadl.viam", "vadl.iss"}
)
@SuppressWarnings({"ParameterName", "ThreadJoinLoop", "OverloadMethodsDeclarationOrder"})
class IssNormalizer implements VadlBuiltInNoStatusDispatcher<BuiltInCall> {
  private final Graph behavior;
  private final int targetSize;
  private final Set<ExpressionNode> handledCache =
      Collections.newSetFromMap(new IdentityHashMap<>());

  public IssNormalizer(Graph behavior, int targetSize) {
    this.behavior = behavior;
    this.targetSize = targetSize;
  }

  void run() {
    // get all root expression nodes to normalize them.
    behavior.getNodes(ExpressionNode.class)
        .filter(n -> n.usages().noneMatch(e -> e instanceof ExpressionNode))
        .forEach(this::normalize);
  }

  private void normalize(ExpressionNode node) {
    if (handledCache.contains(node)) {
      return;
    }
    if (node instanceof IssExtractNode) {
      return;
    }
    normalizeInputs(node);
    IssNormalizerDispatcher.dispatch(this, node);
    handledCache.add(node);
  }

  private void normalizeInputs(ExpressionNode node) {
    node.inputs().forEach(e -> normalize((ExpressionNode) e));
  }

  private IssExtractNode extract(ExpressionNode node, int fromWidth, int toWidth, boolean signed) {
    var nodeSize = node.type().asDataType().bitWidth();
    return extract(node, fromWidth, toWidth, signed, BitsType.bits(nodeSize));
  }

  private IssExtractNode extract(ExpressionNode node, int fromWidth, int toWidth, boolean signed,
                                 BitsType originalType) {
    var tcgExtend = signed ? TcgExtend.SIGN : TcgExtend.ZERO;
    return behavior.add(
        new IssExtractNode(node, tcgExtend, fromWidth, toWidth, originalType));
  }

  private IssExtractNode truncate(ExpressionNode node, int toWidth) {
    return extract(node, toWidth, targetSize, false);
  }

  private IssExtractNode signExtend(ExpressionNode node) {
    return signExtend(node, sizeOf(node));
  }

  private IssExtractNode signExtend(ExpressionNode node, int fromWith) {
    return extract(node, fromWith, targetSize, true);
  }

  private void signExtendArg(BuiltInCall call, int i) {
    signExtendArg(call, i, sizeOf(call.arguments().get(i)));
  }

  private void signExtendArg(BuiltInCall call, int i, int fromWidth) {
    var arg = call.arguments().get(i);
    var signExtended = signExtend(arg, fromWidth);
    call.replaceInput(arg, signExtended);
  }

  private void signExtendAllArgs(BuiltInCall call) {
    for (var i = 0; i < call.arguments().size(); i++) {
      signExtendArg(call, i);
    }
  }

  private void truncateResult(ExpressionNode node, int toWidth) {
    var trunc = truncate(node, toWidth);
    node.replaceAtAllUsages(trunc);
  }

  private void truncateResult(ExpressionNode call) {
    truncateResult(call, sizeOf(call));
  }

  private int sizeOf(ExpressionNode call) {
    return call.type().asDataType().bitWidth();
  }

  /* Leaf nodes that don't require any handling */

  @Handler
  void handle(ConstantNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(FieldRefNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(FieldAccessRefNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(FuncParamNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(ReadRegNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(IssStaticPcRegNode toHandle) {
    // do nothing
  }

  /* Nodes that require manipulation */

  @Handler
  void handle(TupleGetFieldNode toHandle) {
    throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
  }

  @Handler
  void handle(IssMulhNode toHandle) {
    if (toHandle.kind() == IssMulKind.SIGNED_SIGNED) {
      toHandle.replaceInput(
          toHandle.arg2(),
          signExtend(toHandle.arg2())
      );
    }

    if (toHandle.kind() != IssMulKind.UNSIGNED_UNSIGNED) {
      toHandle.replaceInput(
          toHandle.arg1(),
          signExtend(toHandle.arg1())
      );
    }
    truncateResult(toHandle, sizeOf(toHandle));
  }

  @Handler
  void handle(ReadRegFileNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(ReadArtificialResNode toHandle) {
    throw new UnsupportedOperationException("Type ReadArtificialResNode not yet implemented");
  }

  @Handler
  void handle(ReadMemNode toHandle) {
    // do nothing
  }


  @Handler
  void handle(SelectNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(ExprSaveNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(TruncateNode toHandle) {
    toHandle.replaceAndDelete(
        extract(
            toHandle.value(),
            targetSize,
            sizeOf(toHandle),
            false,
            toHandle.type().toBitsType()
        )
    );
  }

  @Handler
  void handle(SignExtendNode toHandle) {
    toHandle.replaceAndDelete(
        extract(
            toHandle.value(),
            toHandle.fromBitWidth(),
            sizeOf(toHandle),
            true,
            toHandle.type().toBitsType()
        )
    );
  }

  @Handler
  void handle(ZeroExtendNode toHandle) {
    toHandle.replaceAndDelete(
        extract(
            toHandle.value(),
            toHandle.fromBitWidth(),
            sizeOf(toHandle),
            false,
            toHandle.type().toBitsType()
        )
    );
  }

  @Handler
  void handle(SliceNode toHandle) {
    throw new UnsupportedOperationException("Type SliceNode not yet implemented");
  }


  @Handler
  void handle(IssMul2Node toHandle) {
    var lhsType = toHandle.arg1().type().asDataType().bitWidth();
    var rhsType = toHandle.arg2().type().asDataType().bitWidth();

    toHandle.inputs().map(ExpressionNode.class::cast).forEach(this::normalize);

    var lhsSigned = toHandle.kind() != IssMulKind.UNSIGNED_UNSIGNED;
    toHandle.replaceInput(
        toHandle.arg1(),
        extract(toHandle.arg1(), lhsType, targetSize, lhsSigned)
    );
    var rhsSigned = toHandle.kind() == IssMulKind.SIGNED_SIGNED;
    toHandle.replaceInput(
        toHandle.arg2(),
        extract(toHandle.arg2(), rhsType, targetSize, rhsSigned)
    );

  }

  /* Handle built-in calls */

  @Handler
  void handle(BuiltInCall toHandle) {
    var couldDispatch = this.dispatch(toHandle, toHandle.builtIn());
    toHandle.ensure(couldDispatch, "Couldn't normalize built-in call. Handler method not defined.");
  }

  @Override
  public void handleNEG(BuiltInCall input) {
    signExtendArg(input, 0);
    truncateResult(input);
  }

  @Override
  public void handleADD(BuiltInCall input) {
    truncateResult(input);
  }

  @Override
  public void handleSSATADD(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleUSATADD(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleSUB(BuiltInCall input) {
    truncateResult(input);
  }

  @Override
  public void handleSSATSUB(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleUSATSUB(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleMUL(BuiltInCall input) {
    truncateResult(input);
  }

  @Override
  public void handleUMULL(BuiltInCall input) {
    truncateResult(input);
  }

  @Override
  public void handleSMULL(BuiltInCall input) {
    signExtendAllArgs(input);
    truncateResult(input);
  }

  @Override
  public void handleSUMULL(BuiltInCall input) {
    signExtendArg(input, 0);
    truncateResult(input);
  }

  @Override
  public void handleSMOD(BuiltInCall input) {
    signExtendAllArgs(input);
    truncateResult(input);
  }

  @Override
  public void handleUMOD(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleSDIV(BuiltInCall input) {
    signExtendAllArgs(input);
    truncateResult(input);
  }

  @Override
  public void handleUDIV(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleNOT(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleAND(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleXOR(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleOR(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleEQU(BuiltInCall input) {
    // equal can operate on the normalized results of its operands without modification
    // do nothing
  }

  @Override
  public void handleNEQ(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleSLTH(BuiltInCall input) {
    signExtendAllArgs(input);
  }

  @Override
  public void handleULTH(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleSLEQ(BuiltInCall input) {
    signExtendAllArgs(input);
  }

  @Override
  public void handleULEQ(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleSGTH(BuiltInCall input) {
    signExtendAllArgs(input);
  }

  @Override
  public void handleUGTH(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleSGEQ(BuiltInCall input) {
    signExtendAllArgs(input);
  }

  @Override
  public void handleUGEQ(BuiltInCall input) {
    // do nothing
  }

  @Override
  public void handleLSL(BuiltInCall input) {
    truncateResult(input);
  }

  @Override
  public void handleASR(BuiltInCall input) {
    signExtendArg(input, 0);
    truncateResult(input);
  }

  @Override
  public void handleLSR(BuiltInCall input) {
    // do nothing, as input and result are unsigned and smaller than the first operand
  }

  @Override
  public void handleROL(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleROR(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleRRX(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleCOB(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleCZB(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleCLZ(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleCLO(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleCLS(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }


  /* Nodes that should not exist at this point */

  @Handler
  void handle(IssExtractNode toHandle) {
    throw graphError(toHandle, "Node should not occur here");
  }

  @Handler
  void handle(FuncCallNode toHandle) {
    throw graphError(toHandle, "Node should not occur here");
  }

  @Handler
  void handle(LetNode toHandle) {
    throw graphError(toHandle, "Node should not occur here");
  }

  @Handler
  void handle(AsmBuiltInCall toHandle) {
    throw graphError(toHandle, "Node should not occur here");
  }

  @Handler
  void handle(ReadStageOutputNode toHandle) {
    throw new UnsupportedOperationException("Type ReadStageOutputNode not yet implemented");
  }

  @FormatMethod
  private ViamError graphError(Node node, String fmt, Object... args) {
    return new ViamGraphError(fmt, args)
        .addContext(node);
  }

}