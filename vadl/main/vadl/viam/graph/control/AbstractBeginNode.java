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

import vadl.viam.graph.GraphNodeVisitor;

/**
 * The AbstractBeginNode represents the start of a control flow.
 * This may be a subflow or the most outer control flow.
 */
public abstract class AbstractBeginNode extends DirectionalNode {

  public AbstractBeginNode(ControlNode next) {
    super(next);
  }

  public AbstractBeginNode() {
  }

  /**
   * Returns if the branch starting with this node is empty.
   * This is the case iff the next node is an end node with no side effects.
   */
  public boolean isEmpty() {
    if (next() instanceof AbstractEndNode end) {
      return end.sideEffects().isEmpty();
    }
    return false;
  }
  

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
