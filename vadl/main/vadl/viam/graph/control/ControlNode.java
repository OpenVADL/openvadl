package vadl.viam.graph.control;

import javax.annotation.Nonnull;
import vadl.viam.graph.Node;

/**
 * Control nodes are part of the control flow graph within the VIAM Graph.
 *
 * <p>They are fixed at position and may not freely move/reorder.
 * </p>
 */
public abstract class ControlNode extends Node {

  /**
   * Inserts a new {@link DirectionalNode} before the current node.
   *
   * @param <T>     the type extending {@link DirectionalNode}
   * @param newNode the new directional node to be inserted
   * @return the inserted node
   */
  public <T extends DirectionalNode> T addBefore(@Nonnull T newNode) {
    ensure(isActive() && graph() != null, "Node is not active");

    var predecessor = predecessor();
    ensure(predecessor instanceof DirectionalNode,
        "Predecessor is not a directional node, but %s", predecessor);

    // the previous directional node can be used to add this after it
    // (so in between of this and its predecessor)
    var prevDir = (DirectionalNode) predecessor;
    return prevDir.addAfter(newNode);
  }

}
