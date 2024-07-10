package vadl.viam.graph.control;


/**
 * The BeginNode class represents the start of a control subflow.
 * An example for such a subflow is an if branch.
 */
public class BeginNode extends AbstractBeginNode {
  public BeginNode(ControlNode next) {
    super(next);
  }
}
