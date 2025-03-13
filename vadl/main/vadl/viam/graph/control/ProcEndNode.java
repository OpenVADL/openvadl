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
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Indicates the end of a {@link vadl.viam.Procedure}.
 */
public class ProcEndNode extends AbstractEndNode {

  public ProcEndNode(
      NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
  }

  @Override
  public ProcEndNode copy() {
    return new ProcEndNode(sideEffects().copy());
  }

  @Override
  public ProcEndNode shallowCopy() {
    return new ProcEndNode(sideEffects());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // do nothing
  }
}
