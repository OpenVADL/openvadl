// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.graph.control;

import javax.annotation.Nullable;

/**
 * Interface for traversing a Control Flow Graph (CFG).
 */
public interface CfgTraverser {

  default ControlNode onControlNode(ControlNode controlNode) {
    return controlNode;
  }

  default ControlNode onDirectional(DirectionalNode dir) {
    return dir;
  }

  default ControlNode onEnd(AbstractEndNode endNode) {
    return endNode;
  }

  default ControlNode onControlSplit(ControlSplitNode controlSplit) {
    return controlSplit;
  }

  /**
   * Traverses the control flow graph starting from the given node.
   */
  default AbstractEndNode traverseBranch(ControlNode branchBegin) {
    ControlNode currNode = branchBegin;

    while (true) {
      currNode = handleControlNode(currNode);

      if (currNode instanceof AbstractEndNode) {
        return (AbstractEndNode) currNode;
      } else if (currNode instanceof DirectionalNode dirNode) {
        currNode = traverseDirectional(dirNode);
      } else if (currNode instanceof ControlSplitNode splitNode) {
        currNode = traverseControlSplit(splitNode);
      } else if (currNode != null) {
        currNode.ensure(false,
            "Expected directional or control split node, but got this node in CFG."
        );
      }
    }
  }

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

  default ControlNode traverseDirectional(DirectionalNode dirNode) {
    return dirNode.next();
  }

  /**
   * Traverses all branches of a control split and returns the merge node reached afterward.
   */
  default ControlNode traverseControlSplit(ControlSplitNode splitNode) {
    @Nullable AbstractEndNode someEnd = null;
    for (var branch : splitNode.branches()) {
      someEnd = traverseBranch(branch);
    }
    splitNode.ensure(someEnd != null, "Control split has no branches.");

    return (MergeNode) someEnd.usages().findFirst().get();
  }
}
