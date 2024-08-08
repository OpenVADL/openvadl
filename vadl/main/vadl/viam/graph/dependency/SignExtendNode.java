package vadl.viam.graph.dependency;

import vadl.types.Type;
import vadl.viam.graph.Node;

public class SignExtendNode extends UnaryNode {

  public SignExtendNode(ExpressionNode value, Type type) {
    super(value, type);
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
