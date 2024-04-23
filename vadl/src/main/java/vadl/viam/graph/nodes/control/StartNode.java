package vadl.viam.graph.nodes.control;

import vadl.viam.graph.Node;

/**
 * Represents the start of a main control flow.
 */
public class StartNode extends AbstractBeginNode {
  public StartNode(Node next) {
    super(next);
  }
}
