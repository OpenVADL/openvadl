package vadl.viam.graph.nodes.control;

import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.Node;
import vadl.viam.graph.nodes.ControlNode;

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
}
