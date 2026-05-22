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
import vadl.iss.passes.extensions.VectorTensorPlan;
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
  private RegisterTensor rhsRegister;
  @Input
  private NodeList<ExpressionNode> rhsAccessorIndices;

  /**
   * Creates one backend gvec operation node from the selected direct-gvec plan payload.
   */
  public TcgGvecOpNode(VectorTensorPlan plan) {
    this(
        plan.op(),
        plan.elementBits(),
        plan.shape().oprszBytes(),
        plan.shape().maxszBytes(),
        plan.destination().registerTensor(),
        new NodeList<>(plan.destination().accessorIndices()),
        requireRegisterOperand(plan, 0),
        new NodeList<>(requireRegisterOperand(plan, 0).accessorIndices()),
        requireRegisterOperand(plan, 1),
        new NodeList<>(requireRegisterOperand(plan, 1).accessorIndices())
    );
  }

  private TcgGvecOpNode(VectorTensorPlan.VectorOp op,
                        int elementBits,
                        int oprszBytes,
                        int maxszBytes,
                        RegisterTensor destinationRegister,
                        NodeList<ExpressionNode> destinationAccessorIndices,
                        VectorTensorPlan.VectorRegisterBinding lhsBinding,
                        NodeList<ExpressionNode> lhsAccessorIndices,
                        VectorTensorPlan.VectorRegisterBinding rhsBinding,
                        NodeList<ExpressionNode> rhsAccessorIndices) {
    this.op = op;
    this.elementBits = elementBits;
    this.oprszBytes = oprszBytes;
    this.maxszBytes = maxszBytes;
    this.destinationRegister = destinationRegister;
    this.destinationAccessorIndices = destinationAccessorIndices;
    this.lhsRegister = lhsBinding.registerTensor();
    this.lhsAccessorIndices = lhsAccessorIndices;
    this.rhsRegister = rhsBinding.registerTensor();
    this.rhsAccessorIndices = rhsAccessorIndices;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return gvecFunctionName()
        + "("
        + memOp()
        + ", "
        + offsetExpr(destinationRegister, destinationAccessorIndices, nodeToCCode)
        + ", "
        + offsetExpr(lhsRegister, lhsAccessorIndices, nodeToCCode)
        + ", "
        + offsetExpr(rhsRegister, rhsAccessorIndices, nodeToCCode)
        + ", "
        + oprszBytes
        + ", "
        + maxszBytes
        + ");";
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
        elementBits,
        oprszBytes,
        maxszBytes,
        destinationRegister,
        destinationAccessorIndices.copy(),
        new VectorTensorPlan.VectorRegisterBinding(lhsRegister, lhsAccessorIndices),
        lhsAccessorIndices.copy(),
        new VectorTensorPlan.VectorRegisterBinding(rhsRegister, rhsAccessorIndices),
        rhsAccessorIndices.copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new TcgGvecOpNode(
        op,
        elementBits,
        oprszBytes,
        maxszBytes,
        destinationRegister,
        destinationAccessorIndices,
        new VectorTensorPlan.VectorRegisterBinding(lhsRegister, lhsAccessorIndices),
        lhsAccessorIndices,
        new VectorTensorPlan.VectorRegisterBinding(rhsRegister, rhsAccessorIndices),
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
    destinationAccessorIndices = destinationAccessorIndices.stream().map(
            (e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
    lhsAccessorIndices = lhsAccessorIndices.stream().map(
            (e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
    rhsAccessorIndices = rhsAccessorIndices.stream().map(
            (e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  private String gvecFunctionName() {
    return switch (op) {
      case ADD -> "tcg_gen_gvec_add";
      case SUB -> "tcg_gen_gvec_sub";
      case AND -> "tcg_gen_gvec_and";
      case OR -> "tcg_gen_gvec_or";
      case XOR -> "tcg_gen_gvec_xor";
      case MUL -> "tcg_gen_gvec_mul";
      case NONE -> throw unsupported("unsupported gvec op NONE");
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

  private static VectorTensorPlan.VectorRegisterBinding requireRegisterOperand(
      VectorTensorPlan plan,
      int operandIndex
  ) {
    var operand = plan.operands().get(operandIndex);
    var binding = operand.registerBinding();
    if (binding == null) {
      throw new ViamGraphError("Missing vector-register binding for operand %s", operandIndex);
    }
    return binding;
  }

  private ViamGraphError unsupported(String message) {
    return new ViamGraphError("%s", message).addContext(this);
  }
}
