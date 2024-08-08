package vadl.viam.graph.dependency;

import vadl.types.Type;
import vadl.viam.graph.Node;

public class ZeroExtendNode extends UnaryNode {

  public ZeroExtendNode(ExpressionNode value, Type type) {
    super(value, type);
  }

  @Override
  public Node copy() {
    return new ZeroExtendNode((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new ZeroExtendNode(value, type());
  }
}
