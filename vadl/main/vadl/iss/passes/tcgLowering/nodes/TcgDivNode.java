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
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represent the {@code tcg_gen_div} and {@code tcg_gen_divu} TCG operations.
 * Depending on {@link #isSigned()} the former or latter is the correct.
 */
public class TcgDivNode extends TcgBinaryOpNode {

  @DataValue
  private boolean signed;

  public TcgDivNode(boolean signed, TcgVRefNode dest, TcgVRefNode arg1,
                    TcgVRefNode arg2) {
    super(dest, arg1, arg2);
    this.signed = signed;
  }

  public boolean isSigned() {
    return signed;
  }

  @Override
  public String tcgFunctionName() {
    var sign = signed ? "" : "u";
    return "tcg_gen_div" + sign;
  }

  @Override
  public Node copy() {
    return new TcgDivNode(signed, firstDest().copy(), arg1().copy(), arg2().copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgDivNode(signed, firstDest(), arg1(), arg2());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(signed);
  }
}
