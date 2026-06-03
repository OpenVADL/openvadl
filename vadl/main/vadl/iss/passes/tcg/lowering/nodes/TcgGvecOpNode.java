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
import vadl.iss.passes.extensions.VectorTensorPlan.OperandForm;
import vadl.iss.passes.extensions.VectorTensorPlan.VectorOp;
import vadl.iss.passes.nodes.IssGvecOpNode;
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
  private VectorOp op;
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

  @Input
  @Nullable
  private TcgVRefNode scalarOperand;

  @Input
  @Nullable
  private ExpressionNode immediateOperand;

  /**
   * Creates one backend gvec operation node from the ISS-level gvec canonical node.
   */
  public TcgGvecOpNode(IssGvecOpNode node, @Nullable TcgVRefNode scalarOperand) {
    this(
        node.op(),
        node.operandForm(),
        node.elementBits(),
        node.oprszBytes(),
        node.maxszBytes(),
        node.destinationRegister(),
        node.destinationAccessorIndices().copy(),
        node.lhsRegister(),
        node.lhsAccessorIndices().copy(),
        node.rhsRegister(),
        node.rhsAccessorIndices().copy(),
        scalarOperand,
        node.immediateOperand() == null ? null : node.immediateOperand().copy()
    );
  }

  private TcgGvecOpNode(VectorOp op,
                        OperandForm operandForm,
                        int elementBits,
                        int oprszBytes,
                        int maxszBytes,
                        RegisterTensor destinationRegister,
                        NodeList<ExpressionNode> destinationAccessorIndices,
                        RegisterTensor lhsRegister,
                        NodeList<ExpressionNode> lhsAccessorIndices,
                        @Nullable RegisterTensor rhsRegister,
                        NodeList<ExpressionNode> rhsAccessorIndices,
                        @Nullable TcgVRefNode scalarOperand,
                        @Nullable ExpressionNode immediateOperand) {
    this.op = op;
    this.operandForm = operandForm;
    this.elementBits = elementBits;
    this.oprszBytes = oprszBytes;
    this.maxszBytes = maxszBytes;
    this.destinationRegister = destinationRegister;
    this.destinationAccessorIndices = destinationAccessorIndices;
    this.lhsRegister = lhsRegister;
    this.lhsAccessorIndices = lhsAccessorIndices;
    this.rhsRegister = rhsRegister;
    this.rhsAccessorIndices = rhsAccessorIndices;
    this.scalarOperand = scalarOperand;
    this.immediateOperand = immediateOperand;
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
      case VECTOR_SCALAR -> gvecScalarFunctionName()
          + "("
          + memOp()
          + ", "
          + offsetExpr(destinationRegister, destinationAccessorIndices, nodeToCCode)
          + ", "
          + offsetExpr(lhsRegister, lhsAccessorIndices, nodeToCCode)
          + ", "
          + requireScalarOperand().cCode()
          + ", "
          + oprszBytes
          + ", "
          + maxszBytes
          + ");";
      case VECTOR_IMMEDIATE -> gvecImmediateFunctionName()
          + "("
          + memOp()
          + ", "
          + offsetExpr(destinationRegister, destinationAccessorIndices, nodeToCCode)
          + ", "
          + offsetExpr(lhsRegister, lhsAccessorIndices, nodeToCCode)
          + ", "
          + nodeToCCode.apply(requireImmediateOperand())
          + ", "
          + oprszBytes
          + ", "
          + maxszBytes
          + ");";
      case SCALAR_BROADCAST, IMMEDIATE_BROADCAST ->
          throw unsupported("unsupported gvec operand form " + operandForm);
    };
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return scalarOperand == null ? Set.of() : Set.of(scalarOperand);
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
        lhsRegister,
        lhsAccessorIndices.copy(),
        rhsRegister,
        rhsAccessorIndices.copy(),
        scalarOperand == null ? null : scalarOperand.copy(),
        immediateOperand == null ? null : immediateOperand.copy()
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
        lhsRegister,
        lhsAccessorIndices,
        rhsRegister,
        rhsAccessorIndices,
        scalarOperand,
        immediateOperand
    );
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(destinationAccessorIndices);
    collection.addAll(lhsAccessorIndices);
    collection.addAll(rhsAccessorIndices);
    if (this.scalarOperand != null) {
      collection.add(scalarOperand);
    }
    if (this.immediateOperand != null) {
      collection.add(immediateOperand);
    }
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
    scalarOperand = visitor.applyNullable(this, scalarOperand, TcgVRefNode.class);
    immediateOperand = visitor.applyNullable(this, immediateOperand, ExpressionNode.class);
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

  private String gvecScalarFunctionName() {
    return switch (op) {
      case ADD -> "tcg_gen_gvec_adds";
      case SUB -> "tcg_gen_gvec_subs";
      case AND -> "tcg_gen_gvec_ands";
      case OR -> "tcg_gen_gvec_ors";
      case XOR -> "tcg_gen_gvec_xors";
      case MUL -> "tcg_gen_gvec_muls";
      case MOV -> throw unsupported("unsupported scalar gvec op " + op);
    };
  }

  private String gvecImmediateFunctionName() {
    return switch (op) {
      case ADD -> "tcg_gen_gvec_addi";
      case AND -> "tcg_gen_gvec_andi";
      case OR -> "tcg_gen_gvec_ori";
      case XOR -> "tcg_gen_gvec_xori";
      case MUL -> "tcg_gen_gvec_muli";
      case MOV, SUB -> throw unsupported("unsupported immediate gvec op " + op);
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

  private RegisterTensor requireRegister(@Nullable RegisterTensor registerTensor,
                                         String role) {
    if (registerTensor == null) {
      throw unsupported("missing " + role + " register operand");
    }
    return registerTensor;
  }

  private TcgVRefNode requireScalarOperand() {
    if (scalarOperand == null) {
      throw unsupported("missing scalar operand");
    }
    return scalarOperand;
  }

  private ExpressionNode requireImmediateOperand() {
    if (immediateOperand == null) {
      throw unsupported("missing immediate operand");
    }
    return immediateOperand;
  }

  private ViamGraphError unsupported(String message) {
    return new ViamGraphError("%s", message).addContext(this);
  }
}
