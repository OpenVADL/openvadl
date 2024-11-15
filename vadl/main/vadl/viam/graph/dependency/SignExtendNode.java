package vadl.viam.graph.dependency;

import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a sign extension of the node's value to the assigned type.
 * This node is constructed during the
 * {@link vadl.viam.passes.typeCastElimination.TypeCastEliminationPass}.
 *
 * @see vadl.viam.passes.typeCastElimination.TypeCastEliminator
 */
public class SignExtendNode extends UnaryNode implements Canonicalizable {

  public SignExtendNode(ExpressionNode value, DataType type) {
    super(value, type);
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(super.type() instanceof DataType, "Type must be a data type");
    ensure(value.type() instanceof DataType, "Value must be a data type");
    ensure(((DataType) value.type()).bitWidth() <= type().bitWidth(),
        "Value's type bit-width must be less or equal node's type");
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append("sext<").append(type().bitWidth()).append(">(");
    value.prettyPrint(sb);
    sb.append(")");
  }

  @Override
  public Node copy() {
    return new SignExtendNode((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new SignExtendNode(value, type());
  }

  @Override
  public Node canonical() {
    if (value instanceof ConstantNode constantNode
        && constantNode.constant instanceof Constant.Value constant) {
      // if the constant node we can zero extend the node
      return new ConstantNode(constant.signExtend(this.type()));
    }
    return this;
  }
}
