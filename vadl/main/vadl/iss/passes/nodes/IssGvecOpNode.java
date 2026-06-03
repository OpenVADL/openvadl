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

package vadl.iss.passes.nodes;

import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.extensions.VectorTensorPlan;
import vadl.iss.passes.extensions.VectorTensorPlan.OperandForm;
import vadl.iss.passes.extensions.VectorTensorPlan.VectorOperand;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * ISS-level canonical node for one direct gvec operation selected from a vector region.
 */
public class IssGvecOpNode extends DependencyNode {

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

  @Input
  @Nullable
  private ExpressionNode scalarOperand;

  @Input
  @Nullable
  private ExpressionNode immediateOperand;

  /**
   * Creates one ISS-level gvec node from the selected direct-gvec plan payload.
   */
  public IssGvecOpNode(VectorTensorPlan plan) {
    this(
        plan.op(),
        plan.operandForm(),
        plan.elementBits(),
        plan.shape().oprszBytes(),
        plan.shape().maxszBytes(),
        plan.destination().registerTensor(),
        new NodeList<>(plan.destination().accessorIndices()),
        requireRegisterBinding(requireOperand(plan, 0), plan, 0),
        new NodeList<>(
            requireRegisterBinding(requireOperand(plan, 0), plan, 0).accessorIndices()
        ),
        optionalRegisterBinding(optionalOperand(plan, 1)),
        rhsAccessorIndicesOf(plan),
        scalarExpressionOf(plan),
        immediateExpressionOf(plan)
    );
  }

  private IssGvecOpNode(VectorTensorPlan.VectorOp op,
                        OperandForm operandForm,
                        int elementBits,
                        int oprszBytes,
                        int maxszBytes,
                        RegisterTensor destinationRegister,
                        NodeList<ExpressionNode> destinationAccessorIndices,
                        VectorTensorPlan.VectorRegisterBinding lhsBinding,
                        NodeList<ExpressionNode> lhsAccessorIndices,
                        @Nullable VectorTensorPlan.VectorRegisterBinding rhsBinding,
                        NodeList<ExpressionNode> rhsAccessorIndices,
                        @Nullable ExpressionNode scalarOperand,
                        @Nullable ExpressionNode immediateOperand) {
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
    this.scalarOperand = scalarOperand;
    this.immediateOperand = immediateOperand;
    validateOperandForm();
  }

  public VectorTensorPlan.VectorOp op() {
    return op;
  }

  public OperandForm operandForm() {
    return operandForm;
  }

  public int elementBits() {
    return elementBits;
  }

  public int oprszBytes() {
    return oprszBytes;
  }

  public int maxszBytes() {
    return maxszBytes;
  }

  public RegisterTensor destinationRegister() {
    return destinationRegister;
  }

  public NodeList<ExpressionNode> destinationAccessorIndices() {
    return destinationAccessorIndices;
  }

  public RegisterTensor lhsRegister() {
    return lhsRegister;
  }

  public NodeList<ExpressionNode> lhsAccessorIndices() {
    return lhsAccessorIndices;
  }

  public @Nullable RegisterTensor rhsRegister() {
    return rhsRegister;
  }

  public NodeList<ExpressionNode> rhsAccessorIndices() {
    return rhsAccessorIndices;
  }

  public @Nullable ExpressionNode scalarOperand() {
    return scalarOperand;
  }

  public @Nullable ExpressionNode immediateOperand() {
    return immediateOperand;
  }

  @Override
  public Node copy() {
    return new IssGvecOpNode(
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
        rhsAccessorIndices.copy(),
        scalarOperand == null ? null : scalarOperand.copy(),
        immediateOperand == null ? null : immediateOperand.copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new IssGvecOpNode(
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
        rhsAccessorIndices,
        scalarOperand,
        immediateOperand
    );
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

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
    scalarOperand = visitor.applyNullable(this, scalarOperand, ExpressionNode.class);
    immediateOperand = visitor.applyNullable(this, immediateOperand, ExpressionNode.class);
  }

  private void validateOperandForm() {
    switch (operandForm) {
      case VECTOR_VECTOR -> ensure(rhsRegister != null && scalarOperand == null
              && immediateOperand == null,
          "VECTOR_VECTOR requires rhs register and forbids scalar/immediate operands.");
      case VECTOR_MOVE -> ensure(rhsRegister == null && scalarOperand == null
              && immediateOperand == null,
          "VECTOR_MOVE requires exactly one vector register operand.");
      case VECTOR_SCALAR -> ensure(rhsRegister == null && scalarOperand != null
              && immediateOperand == null,
          "VECTOR_SCALAR requires exactly one scalar operand expression.");
      case VECTOR_IMMEDIATE -> ensure(rhsRegister == null && scalarOperand == null
              && immediateOperand != null,
          "VECTOR_IMMEDIATE requires exactly one immediate operand expression.");
      case SCALAR_BROADCAST, IMMEDIATE_BROADCAST ->
          throw new ViamGraphError("unsupported ISS gvec operand form %s", operandForm)
              .addContext(this);
    }
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

  private static @Nullable ExpressionNode scalarExpressionOf(VectorTensorPlan plan) {
    return scalarExpressionOf(optionalOperand(plan, 1));
  }

  private static @Nullable ExpressionNode scalarExpressionOf(@Nullable VectorOperand operand) {
    if (operand == null || operand.kind() != VectorTensorPlan.OperandKind.SCALAR_REGISTER) {
      return null;
    }
    return operand.valueExpression();
  }

  private static @Nullable ExpressionNode immediateExpressionOf(VectorTensorPlan plan) {
    return immediateExpressionOf(optionalOperand(plan, 1));
  }

  private static @Nullable ExpressionNode immediateExpressionOf(@Nullable VectorOperand operand) {
    if (operand == null || operand.kind() != VectorTensorPlan.OperandKind.IMMEDIATE) {
      return null;
    }
    return operand.valueExpression();
  }
}
