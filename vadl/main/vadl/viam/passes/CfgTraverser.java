package vadl.viam.passes;

import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.MergeNode;

public interface CfgTraverser {

  default void onControlNode(ControlNode controlNode) {
    // do nothing by default
  }

  default void onDirectional(DirectionalNode dir) {
    // do nothing by default
  }

  default void onEnd(AbstractEndNode endNode) {
    // do nothing by default
  }

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
      } else if (currNode instanceof DirectionalNode direNode) {
        currNode = direNode.next();
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
   * Traverses all branches of the control split node.
   * It will return the next node after the whole control split.
   * This is typically the MergeNode.
   *
   * @param splitNode The ControlSplitNode to process.
   * @return The ControlNode where to continue. This is typically the MergeNode.
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
