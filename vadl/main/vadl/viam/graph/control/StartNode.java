package vadl.viam.graph.control;

import vadl.viam.graph.Node;

/**
 * Represents the start of a main control flow.
 */
public class StartNode extends AbstractBeginNode {
  public StartNode(ControlNode next) {
    super(next);
  }
}
