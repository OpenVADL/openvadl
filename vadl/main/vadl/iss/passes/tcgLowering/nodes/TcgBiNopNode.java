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

import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * A node that doesn’t emit any operation, however is necessary as replacement of some
 * op node that is not being emitted. Without this, the register allocation would have
 * a gap that can't be filled.
 */
public class TcgBiNopNode extends TcgBinaryOpNode {

  public TcgBiNopNode(TcgVRefNode dest, TcgVRefNode arg1, TcgVRefNode arg2) {
    super(dest, arg1, arg2);
  }

  @Override
  public String tcgFunctionName() {
    return "";
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "";
  }

  @Override
  public Node copy() {
    return new TcgBiNopNode(firstDest().copy(), arg1.copy(), arg2.copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgBiNopNode(firstDest(), arg1, arg2);
  }
}
