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
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a select node where at least one option is a TCG node/variable,
 * but the condition is not (constant at translation time).
 * This is translated to a ternary C operation with the result being moved to the destination.
 * {@code tcg_gen_mov(<dest>, <c-cond> ? <true-case> : <false-case> }
 */
public class TcgConstSelectNode extends TcgBinaryOpNode {

  @Input
  private ExpressionNode condition;

  public TcgConstSelectNode(TcgVRefNode resVar, ExpressionNode condition, TcgVRefNode trueCase,
                            TcgVRefNode falseCase) {
    super(resVar, trueCase, falseCase, resVar.width());
    this.condition = condition;
  }

  @Override
  public String tcgFunctionName() {
    throw error("Should not be called");
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_mov_" + width() + "(" + firstDest().cCode() + ", "
        + nodeToCCode.apply(condition)
        + " ? " + arg1().cCode()
        + " : " + arg2().cCode() + ");";
  }

  public ExpressionNode condition() {
    return condition;
  }

  @Override
  public Node copy() {
    return new TcgConstSelectNode(firstDest().copy(),
        condition.copy(),
        arg1.copy(),
        arg2.copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgConstSelectNode(firstDest(), condition, arg1, arg2);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(condition);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.apply(this, condition, ExpressionNode.class);
  }
}
