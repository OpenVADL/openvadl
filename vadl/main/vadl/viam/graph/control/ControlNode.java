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
