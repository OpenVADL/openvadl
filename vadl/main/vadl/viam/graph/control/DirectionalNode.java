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

  protected DirectionalNode() {
  }

  /**
   * The variant if it is possible to directly set the next node construction.
   */
  protected DirectionalNode(@Nonnull ControlNode next) {
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
  public void setNext(@Nullable ControlNode next) {
    this.updatePredecessorOf(this.next, next);
    this.next = next;
  }

  /**
   * Inserts a direction node between this and the next node.
   * If the new node is not yet active, it will be added to the graph.
   *
   * @param newNode node to be inserted.
   */
  public <T extends DirectionalNode> T addAfter(@Nonnull T newNode) {
    ensure(isActive() && graph() != null, "Node is not active");
    if (!newNode.isActive()) {
      newNode = graph().addWithInputs(newNode);
    }
    var next = this.next();
    // remove predecessor of the next node
    this.setNext(null);
    newNode.setNext(next);
    this.setNext(newNode);
    return newNode;
  }


  /**
   * Replaces this node with its successor, and then safely deletes this node.
   *
   * <p>The method ensures that the node to be deleted has a predecessor, then it updates
   * the predecessor's successor to bypass this node. Finally, it safely deletes this node
   * from the graph to maintain consistency.
   */
  public void replaceByNothingAndDelete() {
    ensure(this.predecessor() != null, "Can only remove this node if the predecessor is set!");
    // set successor to successor of predecessor
    var successor = this.next();
    replaceSuccessor(successor, null);
    predecessor().replaceSuccessor(this, successor);
    safeDelete();
  }

  /**
   * This will replace this node by the given node and also links the new node
   * to the next one.
   * <pre>{@code
   * Before:
   * X --->     This    ---> Y
   * After:
   * X ---> Replacement ---> Y
   * }</pre>
   * After linking, this node is deleted.
   *
   * @param replacement The replacement of the current node
   */
  public void replaceAndLinkAndDelete(DirectionalNode replacement) {
    replace(replacement);
    var next = this.next();
    setNext(null);
    replacement.setNext(next);
    safeDelete();
  }

  public ControlNode next() {
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
