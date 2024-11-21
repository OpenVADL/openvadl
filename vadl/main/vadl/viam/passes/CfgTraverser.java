package vadl.viam.passes;

import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.MergeNode;

public abstract class CfgTraverser {

  public abstract void onControlNode(ControlNode controlNode);

  /**
   * Traverses the control flow graph starting from the given branch begin node.
   *
   * @param branchBegin The starting node of the branch to traverse.
   * @return The end node of the traversal.
   */
  public AbstractEndNode traverseBranch(AbstractBeginNode branchBegin) {
    ControlNode currNode = branchBegin;

    while (true) {
      onControlNode(currNode);

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


  /**
   * Traverses all branches of the control split node.
   * It will return the control split's MergeNode.
   *
   * @param splitNode The ControlSplitNode to process.
   * @return The MergeNode corresponding to the control split.
   */
  public MergeNode traverseControlSplit(ControlSplitNode splitNode) {
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
