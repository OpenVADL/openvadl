package vadl.viam.graph.control;


import vadl.viam.graph.Node;

/**
 * The BeginNode class represents the start of a control subflow.
 * An example for such a subflow is an if branch.
 */
public class BeginNode extends AbstractBeginNode {
  public BeginNode(ControlNode next) {
    super(next);
  }

  @Override
  public Node copy() {
    return new BeginNode((ControlNode) next().copy());
  }

  @Override
  public Node shallowCopy() {
    return new BeginNode((ControlNode) next());
  }
}
