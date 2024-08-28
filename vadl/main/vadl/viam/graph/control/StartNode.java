package vadl.viam.graph.control;

import vadl.viam.graph.Node;

/**
 * Represents the start of a main control flow.
 */
public class StartNode extends AbstractBeginNode {
  public StartNode(ControlNode next) {
    super(next);
  }

  @Override
  public Node copy() {
    return new StartNode((ControlNode) next().copy());
  }

  @Override
  public Node shallowCopy() {
    return new StartNode((ControlNode) next());
  }
}
