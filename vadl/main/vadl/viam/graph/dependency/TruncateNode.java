package vadl.viam.graph.dependency;

import vadl.types.Type;
import vadl.viam.graph.Node;

public class TruncateNode extends UnaryNode {

  public TruncateNode(ExpressionNode value, Type type) {
    super(value, type);
  }

  @Override
  public Node copy() {
    return new TruncateNode((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new TruncateNode(value, type());
  }
}
