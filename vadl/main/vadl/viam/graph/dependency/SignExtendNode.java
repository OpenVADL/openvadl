package vadl.viam.graph.dependency;

import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a sign extension of the node's value to the assigned type.
 * This node is constructed during the
 * {@link vadl.viam.passes.typeCastElimination.TypeCastEliminationPass}.
 *
 * @see vadl.viam.passes.typeCastElimination.TypeCastEliminator
 */
public class SignExtendNode extends UnaryNode {

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
    ensure(type() instanceof SIntType, "Sign extension is only possible to SInt.");
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public Node copy() {
    return new SignExtendNode((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new SignExtendNode(value, type());
  }

}
