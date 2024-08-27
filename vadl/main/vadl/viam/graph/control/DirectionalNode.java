package vadl.viam.graph.control;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The DirectionalNode is a type of control flow node with exactly one
 * successor node.
 */
public abstract class DirectionalNode extends ControlNode {

  @Successor
  // even though it is nullable, the next node is not optional!
  private @Nullable ControlNode next;

  DirectionalNode() {
  }

  /**
   * The variant if it is possible to directly set the next node construction.
   */
  DirectionalNode(ControlNode next) {
    this.next = next;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(next != null, "next node must always be set directly after construction of this");
  }

  /**
   * Sets the successor property of this node.
   *
   * <p>It is important that this is done right after creation.
   * The successor field should never be null.
   *
   * @param next the successor of this node
   */
  public void setNext(@Nonnull ControlNode next) {
    this.ensure(this.next == null || next == this.next,
        "successor of DirectionalNode is only allowed to be set once");
    this.updatePredecessorOf(this.next, next);
    this.next = next;
  }

  public Node next() {
    ensure(next != null, "next node is null but must be set!");
    return next;
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    if (this.next != null) {
      collection.add(next);
    }
  }

  @Override
  public void applyOnSuccessorsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnSuccessorsUnsafe(visitor);
    next = visitor.applyNullable(this, next, ControlNode.class);
  }
}
