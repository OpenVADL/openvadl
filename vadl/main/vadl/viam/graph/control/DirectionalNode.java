package vadl.viam.graph.control;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.Node;

/**
 * The DirectionalNode is a type of control flow node with exactly one
 * successor node.
 */
public abstract class DirectionalNode extends ControlNode {

  @Successor
  // even though it is nullable, the next node is not optional!
  protected @Nullable Node next;

  /**
   * Sets the successor property of this node.
   *
   * <p>It is important that this is done right after creation.
   * The successor field should never be null.
   *
   * @param next the successor of this node
   */
  public void setNext(@Nonnull Node next) {
    this.ensure(this.next == null || next == this.next,
        "successor of DirectionalNode is only allowed to be set once");
    this.next = next;
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    if (this.next != null) {
      collection.add(next);
    }
  }
}
