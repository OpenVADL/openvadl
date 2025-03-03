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

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * TcgLookupAndGotoPtr is representing a TCG (Tiny Code Generation)
 * operation node that jumps to the location of the current PC address.
 * If the address is not yet translated to native machine code, it will trigger
 * the translation loop.
 *
 * <p>It translates to {@code lookup_and_goto_ptr();}
 */
public class TcgLookupAndGotoPtr extends TcgNode {

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_lookup_and_goto_ptr();";
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of();
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return List.of();
  }

  @Override
  public Node copy() {
    return new TcgLookupAndGotoPtr();
  }

  @Override
  public Node shallowCopy() {
    return new TcgLookupAndGotoPtr();
  }

}
