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
 * Represents the {@code tcg_gen_ctpop} TCG instruction in the TCG VIAM lowering.
 * The semantics are: {@code t0 = t1 ? clz/ctz(t1) : t2}, where t2 represents the fallback
 * if t1 is 0. In VADL t2 would be the size of t1.
 */
public class TcgCtpopNode extends TcgUnaryOpNode {

  public TcgCtpopNode(TcgVRefNode dest, TcgVRefNode arg) {
    super(dest, arg);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_ctpop_" + width();
  }

  @Override
  public Node copy() {
    return new TcgCtpopNode(firstDest().copy(), arg().copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgCtpopNode(firstDest(), arg());
  }
}
