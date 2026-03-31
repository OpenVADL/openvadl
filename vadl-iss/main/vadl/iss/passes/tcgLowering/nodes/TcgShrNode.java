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
 * Represents a right shift operation in the Tiny Code Generator (TCG).
 * This class extends TcgBinaryImmOpNode to perform a right shift operation
 * on a source variable by a specified immediate value.
 */
public class TcgShrNode extends TcgBinaryOpNode {

  public TcgShrNode(TcgVRefNode res, TcgVRefNode arg, TcgVRefNode shiftAmount) {
    super(res, arg, shiftAmount, res.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_shr";
  }

  @Override
  public Node copy() {
    return new TcgShrNode(firstDest().copy(TcgVRefNode.class),
        arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgShrNode(firstDest(), arg1, arg2);
  }
}
