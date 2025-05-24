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

import static vadl.error.DiagUtils.throwNotAllowed;
import static vadl.utils.GraphUtils.bits;
import static vadl.utils.GraphUtils.intU;
import static vadl.utils.GraphUtils.sub;

import com.google.errorprone.annotations.FormatMethod;
import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.nodes.IssConstExtractNode;
import vadl.iss.passes.nodes.IssGhostCastNode;
import vadl.iss.passes.nodes.IssStaticPcRegNode;
import vadl.iss.passes.nodes.IssValExtractNode;
import vadl.iss.passes.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.opDecomposition.nodes.IssMulKind;
import vadl.iss.passes.opDecomposition.nodes.IssMulhNode;
import vadl.iss.passes.safeResourceRead.nodes.ExprSaveNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.DispatchFor;
import vadl.javaannotations.Handler;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.utils.VadlBuiltInNoStatusDispatcher;
import vadl.viam.Constant;
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
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
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
 * <p>This pass inserts {@link IssConstExtractNode}s where required and replaces all
 * zero/sign extends and truncates by such node.
 * A follow-up pass merges the extract nodes.
 *
 * @see IssConstExtractNode
 * @see IssExtractOptimizationPass
 */
public class IssNormalizationPass extends AbstractIssPass {

  public IssNormalizationPass(IssConfiguration configuration) {
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
    if (node instanceof IssConstExtractNode) {
      return;
    }
    normalizeInputs(node);
    IssNormalizerDispatcher.dispatch(this, node);
    handledCache.add(node);
  }

  private void normalizeInputs(ExpressionNode node) {
    node.inputs().forEach(e -> normalize((ExpressionNode) e));
  }

  private IssConstExtractNode extract(ExpressionNode node, int fromWidth, int toWidth,
                                      boolean signed) {
    var nodeSize = node.type().asDataType().bitWidth();
    return extract(node, fromWidth, toWidth, signed, BitsType.bits(nodeSize));
  }

  private IssConstExtractNode extract(ExpressionNode node, int fromWidth, int toWidth,
                                      boolean signed,
                                      BitsType originalType) {
    var tcgExtend = signed ? TcgExtend.SIGN : TcgExtend.ZERO;
    return behavior.add(
        new IssConstExtractNode(node, tcgExtend, fromWidth, toWidth, originalType));
  }

  private IssConstExtractNode truncate(ExpressionNode node, int toWidth) {
    return extract(node, toWidth, targetSize, false);
  }

  private IssConstExtractNode signExtend(ExpressionNode node) {
    return signExtend(node, sizeOf(node));
  }

  private IssConstExtractNode signExtend(ExpressionNode node, int fromWith) {
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
  void handle(ReadRegTensorNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(IssStaticPcRegNode toHandle) {
    // do nothing
  }

  @Handler
  void handle(LabelNode toHandle) {
    // do nothing
  }

  /* Nodes that require manipulation */

  @Handler
  void handle(TupleGetFieldNode toHandle) {
    throwNotAllowed(toHandle, "Tuple GetFieldNode");
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
  void handle(ReadArtificialResNode toHandle) {
    // do nothing
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
  void handle(SliceNode node) {
    // is handled in the operation lowering
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
    truncateResult(input);
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

  private boolean isPowerOfTwo(int val) {
    return val > 0 && (val & (val - 1)) == 0;
  }

  /**
   * For shift operations, the second argument is always normalized by (b % N) to
   * ensure defined behavior.
   */
  private void normalizeShiftAmountOperand(BuiltInCall input) {
    var val = input.arguments().getFirst();
    var valT = val.type().asDataType();
    var shift = input.arguments().get(1);
    var shiftT = shift.type().asDataType();
    var minWidth = BitsType.minimalRequiredWidthFor(input.type().asDataType().bitWidth());
    if (shiftT.bitWidth() < minWidth) {
      // the shift cannot wrap around the value
      return;
    }

    if (isPowerOfTwo(valT.bitWidth())) {
      // b % N == b & (N - 1)
      var rhs = Constant.Value.of(valT.bitWidth() - 1, shiftT).toNode();
      // b -> b & (N - 1)
      shift.replace(BuiltInTable.AND.call(shift, rhs));
      return;
    }

    // replace by modulo (b -> b % N)
    var nVal = Constant.Value.of(valT.bitWidth(), shiftT).toNode();
    shift.replace(BuiltInTable.UMOD.call(shift, nVal));
  }

  @Override
  public void handleLSL(BuiltInCall input) {
    normalizeShiftAmountOperand(input);
    truncateResult(input);
  }

  @Override
  public void handleASR(BuiltInCall input) {
    signExtendArg(input, 0);
    normalizeShiftAmountOperand(input);
    truncateResult(input);
  }

  @Override
  public void handleLSR(BuiltInCall input) {
    normalizeShiftAmountOperand(input);
    // do nothing, as input and result are unsigned and smaller than the first operand
  }

  @Override
  @SuppressWarnings("LocalVariableName")
  public void handleROL(BuiltInCall input) {
    var opWidth = input.type().asDataType().bitWidth();
    if (opWidth == targetSize) {
      // if the operation is on target size, there is nothing to do
      return;
    }

    // rotation operation where
    // a = value
    // b = shift amount
    // N = size of value
    // r = (b % N)
    // (a << r) | (a >> (N - r))

    var a = input.arguments().getFirst();
    var b = input.arguments().get(1);
    var N = Constant.Value.of(opWidth, Type.bits(32)).toNode();
    // b % N
    var r = BuiltInTable.UMOD.call(b, N);
    // a << r
    var aLsrR = BuiltInTable.LSL.call(a, r);
    // N - r
    var Nr = BuiltInTable.SUB.call(N, r);
    // a >> Nr
    var aNr = BuiltInTable.LSR.call(a, Nr);
    // aLsrR | aNr
    var result = BuiltInTable.OR.call(aLsrR, aNr);
    // replace call
    result = input.replaceAndDelete(result);
    // truncate result to operation width, as a << r might cause an overflow
    truncate(result, opWidth);
  }

  @Override
  @SuppressWarnings("LocalVariableName")
  public void handleROR(BuiltInCall input) {
    var opWidth = input.type().asDataType().bitWidth();
    if (opWidth == targetSize) {
      // if the operation is on target size, there is nothing to do
      return;
    }

    // rotation operation where
    // a = value
    // b = shift amount
    // N = size of value
    // r = (b % N)
    // (a >> r) | (a << (N - r))

    var a = input.arguments().getFirst();
    var b = input.arguments().get(1);
    var N = Constant.Value.of(opWidth, Type.bits(32)).toNode();
    // b % N
    var r = BuiltInTable.UMOD.call(b, N);
    // a >> r
    var aLsrR = BuiltInTable.LSR.call(a, r);
    // N - r
    var Nr = BuiltInTable.SUB.call(N, r);
    // a << Nr
    var aNr = BuiltInTable.LSL.call(a, Nr);
    // aLsrR | aNr
    var result = BuiltInTable.OR.call(aLsrR, aNr);
    // replace call
    result = input.replaceAndDelete(result);
    // truncate result to operation width, as a << (N - r) might cause an overflow
    truncate(result, opWidth);
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
    var val = input.arguments().get(0);
    var valT = val.type().asDataType();
    if (valT.bitWidth() == targetSize) {
      return;
    }
    // if val is smaller than target size, we know that the target size leading
    // bits until the msb of val will be null.
    // e.g. 0010 -> 4 - 2 = 2
    var constLeadingZ = targetSize - valT.bitWidth();
    // replace original by subtraction that subtracts constant leading zeros from the target size
    // clz result.
    input.replace(BuiltInTable.SUB.call(
        input,
        bits(constLeadingZ, input.type().asDataType().bitWidth()).toNode()
    ));
  }

  @Override
  public void handleCLO(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleCLS(BuiltInCall input) {
    // we replace CLS as there is no such TCG operation.
    // it is replaced by transformations that allow us to use CLZ (count leading zeros)
    // instead.
    var arg = input.arguments().getFirst();
    var argT = arg.type().asDataType();
    var resT = input.type().asDataType();
    if (argT.bitWidth() <= 1) {
      // if <= 1 replace by 0
      input.replaceAndDelete(intU(0, resT.bitWidth()).toNode());
      return;
    }

    var bitWidth = argT.bitWidth();
    var sign = new SliceNode(
        arg,
        Constant.BitSlice.of(bitWidth - 1, bitWidth - 1),
        Type.bits(1)
    );
    // v = sign ? ~a : a;
    var v = new SelectNode(sign, GraphUtils.not(arg), arg);
    ExpressionNode result = BuiltInTable.CLZ.call(v);

    // leading zeros that are known to be zero on a target size operation.
    // this is only non-zero for cls operating on < targetSize.
    // N - 1 because we don't count the sign bit itself
    var guaranteedZeros = targetSize - (bitWidth - 1);
    if (guaranteedZeros > 0) {
      result = sub(result, intU(guaranteedZeros, bitWidth).toNode());
    }

    // replace CLS
    input.replaceAndDelete(result);
  }

  @Override
  public void handleCTZ(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");

  }

  @Override
  public void handleCTO(BuiltInCall input) {
    throw graphError(input, "Normalization not yet implemented for this built-in");
  }

  @Override
  public void handleConcat(BuiltInCall input) {
    // do nothing (result is already fine)
  }


  /* Nodes that should not exist at this point */

  @Handler
  void handle(IssConstExtractNode toHandle) {
    throw graphError(toHandle, "Node should not occur here");
  }

  @Handler
  void handle(IssValExtractNode toHandle) {
    throw graphError(toHandle, "Node should not occur here");
  }

  @Handler
  void handle(IssGhostCastNode toHandle) {
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