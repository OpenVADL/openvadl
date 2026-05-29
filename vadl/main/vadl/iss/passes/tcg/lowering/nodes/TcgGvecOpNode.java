// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.tcg.lowering.nodes;

import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.iss.passes.extensions.VectorTensorPlan;
import vadl.iss.passes.extensions.VectorTensorPlan.OperandForm;
import vadl.iss.passes.extensions.VectorTensorPlan.VectorOperand;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Backend graph node for one direct gvec operation emitted from the lowered behavior graph.
 */
public class TcgGvecOpNode extends TcgNode {

  @DataValue
  private VectorTensorPlan.VectorOp op;
  @DataValue
  private OperandForm operandForm;
  @DataValue
  private int elementBits;
  @DataValue
  private int oprszBytes;
  @DataValue
  private int maxszBytes;

  @DataValue
  private RegisterTensor destinationRegister;
  @Input
  private NodeList<ExpressionNode> destinationAccessorIndices;

  @DataValue
  private RegisterTensor lhsRegister;
  @Input
  private NodeList<ExpressionNode> lhsAccessorIndices;

  @DataValue
  @Nullable
  private RegisterTensor rhsRegister;
  @Input
  private NodeList<ExpressionNode> rhsAccessorIndices;

  /**
   * Creates one backend gvec operation node from the selected direct-gvec plan payload.
   */
  public TcgGvecOpNode(VectorTensorPlan plan) {
    this(
        plan.op(),
        plan.operandForm(),
        plan.elementBits(),
        plan.shape().oprszBytes(),
        plan.shape().maxszBytes(),
        plan.destination().registerTensor(),
        new NodeList<>(plan.destination().accessorIndices()),
        requireRegisterBinding(requireOperand(plan, 0), plan, 0),
        new NodeList<>(requireRegisterBinding(requireOperand(plan, 0), plan, 0).accessorIndices()),
        optionalRegisterBinding(optionalOperand(plan, 1)),
        rhsAccessorIndicesOf(plan)
    );
  }

  private TcgGvecOpNode(VectorTensorPlan.VectorOp op,
                        OperandForm operandForm,
                        int elementBits,
                        int oprszBytes,
                        int maxszBytes,
                        RegisterTensor destinationRegister,
                        NodeList<ExpressionNode> destinationAccessorIndices,
                        VectorTensorPlan.VectorRegisterBinding lhsBinding,
                        NodeList<ExpressionNode> lhsAccessorIndices,
                        @Nullable VectorTensorPlan.VectorRegisterBinding rhsBinding,
                        NodeList<ExpressionNode> rhsAccessorIndices) {
    this.op = op;
    this.operandForm = operandForm;
    this.elementBits = elementBits;
    this.oprszBytes = oprszBytes;
    this.maxszBytes = maxszBytes;
    this.destinationRegister = destinationRegister;
    this.destinationAccessorIndices = destinationAccessorIndices;
    this.lhsRegister = lhsBinding.registerTensor();
    this.lhsAccessorIndices = lhsAccessorIndices;
    this.rhsRegister = rhsBinding == null ? null : rhsBinding.registerTensor();
    this.rhsAccessorIndices = rhsAccessorIndices;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return switch (operandForm) {
      case VECTOR_VECTOR -> gvecFunctionName()
          + "("
          + memOp()
          + ", "
          + offsetExpr(destinationRegister, destinationAccessorIndices, nodeToCCode)
          + ", "
          + offsetExpr(lhsRegister, lhsAccessorIndices, nodeToCCode)
          + ", "
          + offsetExpr(requireRegister(rhsRegister, "rhs"), rhsAccessorIndices, nodeToCCode)
          + ", "
          + oprszBytes
          + ", "
          + maxszBytes
          + ");";
      case VECTOR_MOVE -> "tcg_gen_gvec_mov("
          + memOp()
          + ", "
          + offsetExpr(destinationRegister, destinationAccessorIndices, nodeToCCode)
          + ", "
          + offsetExpr(lhsRegister, lhsAccessorIndices, nodeToCCode)
          + ", "
          + oprszBytes
          + ", "
          + maxszBytes
          + ");";
      case VECTOR_SCALAR, VECTOR_IMMEDIATE, SCALAR_BROADCAST, IMMEDIATE_BROADCAST ->
          throw unsupported("unsupported gvec operand form " + operandForm);
    };
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of();
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return List.of();
  }

  @Override
  public Node copy() {
    return new TcgGvecOpNode(
        op,
        operandForm,
        elementBits,
        oprszBytes,
        maxszBytes,
        destinationRegister,
        destinationAccessorIndices.copy(),
        new VectorTensorPlan.VectorRegisterBinding(lhsRegister, lhsAccessorIndices),
        lhsAccessorIndices.copy(),
        rhsRegister == null ? null : new VectorTensorPlan.VectorRegisterBinding(
            rhsRegister,
            rhsAccessorIndices
        ),
        rhsAccessorIndices.copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new TcgGvecOpNode(
        op,
        operandForm,
        elementBits,
        oprszBytes,
        maxszBytes,
        destinationRegister,
        destinationAccessorIndices,
        new VectorTensorPlan.VectorRegisterBinding(lhsRegister, lhsAccessorIndices),
        lhsAccessorIndices,
        rhsRegister == null ? null : new VectorTensorPlan.VectorRegisterBinding(
            rhsRegister,
            rhsAccessorIndices
        ),
        rhsAccessorIndices
    );
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(destinationAccessorIndices);
    collection.addAll(lhsAccessorIndices);
    collection.addAll(rhsAccessorIndices);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(op);
    collection.add(operandForm);
    collection.add(elementBits);
    collection.add(oprszBytes);
    collection.add(maxszBytes);
    collection.add(destinationRegister);
    collection.add(lhsRegister);
    collection.add(rhsRegister);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    destinationAccessorIndices =
        rewriteNodeList(destinationAccessorIndices, visitor, ExpressionNode.class);
    lhsAccessorIndices = rewriteNodeList(lhsAccessorIndices, visitor, ExpressionNode.class);
    rhsAccessorIndices = rewriteNodeList(rhsAccessorIndices, visitor, ExpressionNode.class);
  }

  private String gvecFunctionName() {
    return switch (op) {
      case MOV -> "tcg_gen_gvec_mov";
      case ADD -> "tcg_gen_gvec_add";
      case SUB -> "tcg_gen_gvec_sub";
      case AND -> "tcg_gen_gvec_and";
      case OR -> "tcg_gen_gvec_or";
      case XOR -> "tcg_gen_gvec_xor";
      case MUL -> "tcg_gen_gvec_mul";
    };
  }

  private String memOp() {
    return switch (elementBits) {
      case 8 -> "MO_8";
      case 16 -> "MO_16";
      case 32 -> "MO_32";
      case 64 -> "MO_64";
      default -> throw unsupported("unsupported gvec element width " + elementBits);
    };
  }

  private String offsetExpr(RegisterTensor registerTensor,
                            List<ExpressionNode> accessorIndices,
                            Function<Node, String> nodeToCCode) {
    var helperName = regInfo(registerTensor).gvecOffsetHelperName();
    if (accessorIndices.isEmpty()) {
      return helperName + "(ctx)";
    }
    var args = accessorIndices.stream()
        .map(nodeToCCode)
        .collect(Collectors.joining(", "));
    return helperName + "(ctx, " + args + ")";
  }

  private static VectorOperand requireOperand(VectorTensorPlan plan, int operandIndex) {
    if (operandIndex >= plan.operands().size()) {
      throw new ViamGraphError("Missing operand %s for gvec plan", operandIndex);
    }
    return plan.operands().get(operandIndex);
  }

  private static @Nullable VectorOperand optionalOperand(
      VectorTensorPlan plan,
      int operandIndex
  ) {
    return operandIndex < plan.operands().size() ? plan.operands().get(operandIndex) : null;
  }

  private static VectorTensorPlan.VectorRegisterBinding requireRegisterBinding(
      VectorOperand operand,
      VectorTensorPlan plan,
      int operandIndex
  ) {
    var binding = operand.registerBinding();
    if (binding == null) {
      throw new ViamGraphError(
          "Missing vector-register binding for operand %s in %s form",
          operandIndex,
          plan.operandForm()
      );
    }
    return binding;
  }

  private static @Nullable VectorTensorPlan.VectorRegisterBinding optionalRegisterBinding(
      @Nullable VectorOperand operand
  ) {
    return operand == null ? null : operand.registerBinding();
  }

  private static NodeList<ExpressionNode> rhsAccessorIndicesOf(VectorTensorPlan plan) {
    var rhsOperand = optionalOperand(plan, 1);
    if (rhsOperand == null || rhsOperand.registerBinding() == null) {
      return new NodeList<>();
    }
    return new NodeList<>(rhsOperand.registerBinding().accessorIndices());
  }

  private RegisterTensor requireRegister(@Nullable RegisterTensor registerTensor,
                                         String role) {
    if (registerTensor == null) {
      throw unsupported("missing " + role + " register operand");
    }
    return registerTensor;
  }

  private ViamGraphError unsupported(String message) {
    return new ViamGraphError("%s", message).addContext(this);
  }
}
