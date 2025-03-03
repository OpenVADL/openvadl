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
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents the TCG setcond operation that 1 or 0 to dest depending on the
 * comparison of its operands.
 * The cond property defines what comparison operator to use.
 */
public class TcgSetCond extends TcgBinaryOpNode {

  @DataValue
  private TcgCondition cond;

  /**
   * This constructor initializes a TcgSetCond object, representing a setcond operation in TCG.
   *
   * @param dest the variable that will store the result of the conditional set operation
   * @param arg1 the first argument variable for the comparison
   * @param arg2 the second argument variable for the comparison
   * @param cond the condition to be evaluated (e.g., EQ, NE, LT, etc.), determining
   *             the result of the comparison
   */
  public TcgSetCond(TcgVRefNode dest, TcgVRefNode arg1, TcgVRefNode arg2, TcgCondition cond) {
    super(dest, arg1, arg2, dest.width());
    this.cond = cond;
  }

  public TcgCondition condition() {
    return cond;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "_" + width() + "("
        + cond.cCode() + ", "
        + firstDest().varName() + ", "
        + arg1.varName() + ", "
        + arg2.varName()
        + ");";
  }

  @Override
  public Node copy() {
    return new TcgSetCond(firstDest().copy(TcgVRefNode.class), arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class), cond);
  }

  @Override
  public Node shallowCopy() {
    return new TcgSetCond(firstDest(), arg1, arg2, cond);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_setcond";
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(cond);
  }
}
