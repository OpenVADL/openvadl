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

package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_gen_not} TCG instruction in the TCG VIAM lowering.
 */
public class TcgNotNode extends TcgUnaryOpNode {

  public TcgNotNode(TcgVRefNode resVar, TcgVRefNode arg1) {
    super(resVar, arg1);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_not_" + firstDest().width();
  }

  @Override
  public Node copy() {
    return new TcgNotNode(firstDest().copy(TcgVRefNode.class), arg.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgNotNode(firstDest(), arg);
  }
}
