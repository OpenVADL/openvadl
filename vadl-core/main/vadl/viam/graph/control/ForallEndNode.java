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

package vadl.viam.graph.control;

import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The end node to the corresponding {@link ForallNode}.
 * It has a single {@link BranchEndNode} as input.
 *
 * @see <a href="https://github.com/OpenVADL/openvadl/pull/566">Github PR #566</a>
 */
public class ForallEndNode extends MergeNode {

  public ForallEndNode(BranchEndNode endNode) {
    super(new NodeList<>(endNode));
  }

  public ForallEndNode(BranchEndNode endNode, ControlNode next) {
    super(new NodeList<>(endNode), next);
  }


  public BranchEndNode endNode() {
    return branchEnds.getFirst();
  }

  public NodeList<SideEffectNode> sideEffects() {
    return endNode().sideEffects();
  }

  @Override
  public ForallEndNode copy() {
    return new ForallEndNode(
        endNode().copy(),
        next().copy(ControlNode.class)
    );
  }

  @Override
  public ForallEndNode shallowCopy() {
    return new ForallEndNode(endNode(), next());
  }

}
