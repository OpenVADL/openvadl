package vadl.viam.graph.control;

import vadl.viam.graph.Node;


/**
 * The BeginNode class represents the start of a control subflow.
 * An example for such a subflow is an if branch.
 */
public class BeginNode extends AbstractBeginNode {
  public BeginNode(Node next) {
    super(next);
  }
}
