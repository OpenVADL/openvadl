package vadl.viam.graph.control;

import java.util.List;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.Node;

/**
 * The DirectionalNode is a type of control flow node with exactly one
 * successor node.
 */
public abstract class DirectionalNode extends ControlNode {

  @Successor
  protected Node next;

  public DirectionalNode(Node next) {
    this.next = next;
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    collection.add(next);
  }
}
