package vadl.viam.graph.control;

import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The AbstractBeginNode represents the start of a control flow.
 * This may be a subflow or the most outer control flow.
 */
public class AbstractBeginNode extends DirectionalNode {

  public AbstractBeginNode(AbstractControlNode next) {
    setNext(next);
  }

  public AbstractBeginNode() {
  }

  @Override
  public Node copy() {
    return new AbstractBeginNode(next);
  }
}
