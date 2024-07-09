package vadl.viam.graph.control;

import java.util.Objects;
import vadl.viam.graph.Node;

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
    return new AbstractBeginNode((AbstractControlNode) Objects.requireNonNull(next).copy());
  }

  @Override
  public Node shallowCopy() {
    return new AbstractBeginNode(Objects.requireNonNull(next));
  }
}
