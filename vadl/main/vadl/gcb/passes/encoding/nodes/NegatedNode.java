package vadl.gcb.passes.encoding.nodes;

import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.UnaryNode;

/**
 * Represents a negative value in the graph. This helps us to remove subtraction in the graph.
 */
public class NegatedNode extends UnaryNode {
  public NegatedNode(ExpressionNode value, Type type) {
    super(value, type);
  }

  @Override
  public Node copy() {
    return new NegatedNode((ExpressionNode) value.copy(),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new NegatedNode(value, type());
  }
}
