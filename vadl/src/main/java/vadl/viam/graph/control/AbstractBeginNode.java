package vadl.viam.graph.control;

import vadl.viam.graph.Node;

/**
 * The AbstractBeginNode represents the start of a control flow.
 * This may be a subflow or the most outer control flow.
 */
public class AbstractBeginNode extends DirectionalNode {
  public AbstractBeginNode(Node next) {
    super(next);
  }
}
