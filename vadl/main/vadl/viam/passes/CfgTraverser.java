// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.viam.passes;

import javax.annotation.Nullable;
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
  default ControlNode onControlNode(ControlNode controlNode) {
    // do nothing by default
    return controlNode;
  }

  /**
   * Called when a {@link DirectionalNode} is encountered during traversal.
   *
   * @param dir The directional node being processed.
   */
  default ControlNode onDirectional(DirectionalNode dir) {
    // do nothing by default
    return dir;
  }

  /**
   * Called when an {@link AbstractEndNode} is encountered during traversal.
   *
   * @param endNode The end node being processed.
   */
  default ControlNode onEnd(AbstractEndNode endNode) {
    // do nothing by default
    return endNode;
  }

  /**
   * Called when a {@link ControlSplitNode} is encountered during traversal.
   *
   * @param controlSplit The control split node being processed.
   */
  default ControlNode onControlSplit(ControlSplitNode controlSplit) {
    // do nothing by default
    return controlSplit;
  }


  /**
   * Traverses the control flow graph starting from the given node.
   *
   * @param branchBegin The node to begin traversing.
   * @return The end node of the traversal.
   */
  default AbstractEndNode traverseBranch(ControlNode branchBegin) {
    ControlNode currNode = branchBegin;

    while (true) {
      // The handleControlNode method might delete the currNode, so we would lose the next node.
      // That's why save it first.
      currNode = handleControlNode(currNode);

      if (currNode instanceof AbstractEndNode) {
        // When we find the end node, we return it
        return (AbstractEndNode) currNode;
      } else if (currNode instanceof DirectionalNode dirNode) {
        currNode = traverseDirectional(dirNode);
      } else if (currNode instanceof ControlSplitNode splitNode) {
        // Handle all branches of the nested control split node
        currNode = traverseControlSplit(splitNode);
      } else if (currNode != null) {
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
   * @return the next node because the handlers might delete the {@code controlNode}.
   */
  private ControlNode handleControlNode(ControlNode controlNode) {
    if (controlNode instanceof DirectionalNode direNode) {
      controlNode = onDirectional(direNode);
    } else if (controlNode instanceof ControlSplitNode splitNode) {
      controlNode = onControlSplit(splitNode);
    } else if (controlNode instanceof AbstractEndNode endNode) {
      controlNode = onEnd(endNode);
    }

    controlNode = onControlNode(controlNode);

    return controlNode;
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

    // Get the merge node from the end of the branch
    return (MergeNode) someEnd.usages().findFirst().get();
  }

}