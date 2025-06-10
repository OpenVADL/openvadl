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
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * The end node to the corresponding {@link ForallNode}.
 * As the {@link ForallNode} is not diverging control flow, this is not an end node,
 * but just a directional node.
 */
public class ForallEndNode extends DirectionalNode {

  public ForallEndNode(@Nonnull ControlNode next) {
    super(next);
  }

  @Override
  public Node copy() {
    return new ForallEndNode(next().copy(ControlNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new ForallEndNode(next());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }
}
