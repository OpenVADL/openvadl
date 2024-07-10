package vadl.viam.graph.control;

/**
 * Represents the start of a main control flow.
 */
public class StartNode extends AbstractBeginNode {
  public StartNode(ControlNode next) {
    super(next);
  }
}
