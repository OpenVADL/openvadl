package vadl.viam.graph.control;

import java.util.Objects;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * The AbstractBeginNode represents the start of a control flow.
 * This may be a subflow or the most outer control flow.
 */
public abstract class AbstractBeginNode extends DirectionalNode {

  public AbstractBeginNode(ControlNode next) {
    super(next);
  }

  public AbstractBeginNode() {
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
