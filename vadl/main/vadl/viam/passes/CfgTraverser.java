package vadl.viam.passes;

import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.MergeNode;

/**
 * Interface for traversing a Control Flow Graph (CFG).
 * Implementations can define custom behaviors by overriding the default methods provided.
 */
public interface CfgTraverser {

  /**
   * Called when a {@link ControlNode} is encountered during traversal.
   * This method is invoked for every control node, after more specific methods
   * like {@link #onDirectional(DirectionalNode)}, {@link #onControlSplit(ControlSplitNode)},
   * or {@link #onEnd(AbstractEndNode)} are called.
   *
   * @param controlNode The control node being processed.
   */
  default void onControlNode(ControlNode controlNode) {
    // do nothing by default
  }

  /**
   * Called when a {@link DirectionalNode} is encountered during traversal.
   *
   * @param dir The directional node being processed.
   */
  default void onDirectional(DirectionalNode dir) {
    // do nothing by default
  }

  /**
   * Called when an {@link AbstractEndNode} is encountered during traversal.
   *
   * @param endNode The end node being processed.
   */
  default void onEnd(AbstractEndNode endNode) {
    // do nothing by default
  }

  /**
   * Called when a {@link ControlSplitNode} is encountered during traversal.
   *
   * @param controlSplit The control split node being processed.
   */
  default void onControlSplit(ControlSplitNode controlSplit) {
    // do nothing by default
  }

  /**
   * Traverses the control flow graph starting from the given branch begin node.
   *
   * @param branchBegin The starting node of the branch to traverse.
   * @return The end node of the traversal.
   */
  default AbstractEndNode traverseBranch(AbstractBeginNode branchBegin) {
    ControlNode currNode = branchBegin;

    while (true) {
      handleControlNode(currNode);

      if (currNode instanceof AbstractEndNode) {
        // When we find the end node, we return it
        return (AbstractEndNode) currNode;
      } else if (currNode instanceof DirectionalNode dirNode) {
        currNode = traverseDirectional(dirNode);
      } else if (currNode instanceof ControlSplitNode splitNode) {
        // Handle all branches of the nested control split node
        currNode = traverseControlSplit(splitNode);
      } else {
        currNode.ensure(false,
            "Expected directional or control split node, but got this node in CFG."
        );
      }
    }
  }

  /**
   * Handles the processing of a {@link ControlNode}. This method calls
   * more specific methods based on the type of the control node.
   *
   * @param controlNode The control node to handle.
   */
  private void handleControlNode(ControlNode controlNode) {
    if (controlNode instanceof DirectionalNode direNode) {
      onDirectional(direNode);
    } else if (controlNode instanceof ControlSplitNode splitNode) {
      onControlSplit(splitNode);
    } else if (controlNode instanceof AbstractEndNode endNode) {
      onEnd(endNode);
    }

    onControlNode(controlNode);
  }

  /**
   * Traverses a {@link DirectionalNode} by moving to its next node.
   *
   * @param dirNode The directional node to traverse.
   * @return The next control node in the traversal.
   */
  default ControlNode traverseDirectional(DirectionalNode dirNode) {
    return dirNode.next();
  }

  /**
   * Traverses all branches of the given {@link ControlSplitNode}. It will return
   * the next node after the entire control split, which is typically a {@link MergeNode}.
   *
   * @param splitNode The control split node to process.
   * @return The control node where to continue after the split, typically a {@link MergeNode}.
   */
  default ControlNode traverseControlSplit(ControlSplitNode splitNode) {
    @Nullable AbstractEndNode someEnd = null;
    for (var branch : splitNode.branches()) {
      someEnd = traverseBranch(branch);
    }
    splitNode.ensure(someEnd != null, "Control split has no branches.");
    splitNode.ensure(someEnd.usageCount() == 1, "End should have exactly one usage: MergeNode");
    // Get the merge node from the end of the branch
    return (MergeNode) someEnd.usages().findFirst().get();
  }

}