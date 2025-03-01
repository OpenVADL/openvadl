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
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_temp_free} TCG function.
 */
public class TcgFreeTemp extends TcgVarNode {

  public TcgFreeTemp(TcgVRefNode variable) {
    super(variable);
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(variable().var().kind() == TcgV.Kind.TMP, "Can only free temporary variables");
  }

  @Override
  public Node copy() {
    return new TcgFreeTemp(variable().copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgFreeTemp(variable());
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_temp_free_i" + variable().width().width + "(" + variable().varName() + ");";
  }
}
