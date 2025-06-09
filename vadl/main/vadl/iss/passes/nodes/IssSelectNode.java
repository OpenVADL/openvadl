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

package vadl.iss.passes.nodes;

import java.util.List;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SelectNode;

/**
 * The lowering of the {@link SelectNode}. It contains a {@link TcgCondition} as operation
 * condition.
 * This is the preparation for the lowering to
 * {@link vadl.iss.passes.tcgLowering.nodes.TcgMovCondNode}.
 * The node is created in the {@link vadl.iss.passes.IssTcgSchedulingPass} and substitutes
 * select nodes that were scheduled.
 *
 * <p>This node is never rendered as C-code, as a {@link SelectNode} that was not scheduled
 * is not turned into a {@link IssSelectNode}.
 * If the original node was scheduled, this node is turned into a
 * {@link vadl.iss.passes.tcgLowering.nodes.TcgMovCondNode} or
 * {@link vadl.iss.passes.tcgLowering.nodes.TcgConstSelectNode}</p>
 */
public class IssSelectNode extends ExpressionNode {

  @DataValue
  private TcgCondition condition;

  @Input
  private ExpressionNode c1;
  @Input
  private ExpressionNode c2;

  @Input
  private ExpressionNode trueCase;
  @Input
  private ExpressionNode falseCase;


  /**
   * Constructs the IssSelectNode.
   *
   * @param condition the tcg condition to use
   * @param c1        the first condition argument (lhs)
   * @param c2        the second condition argument (rhs)
   * @param trueCase  the expression in case of the condition being true
   * @param falseCase the expression in case of the condition being false
   */
  public IssSelectNode(
      TcgCondition condition,
      ExpressionNode c1,
      ExpressionNode c2,
      ExpressionNode trueCase,
      ExpressionNode falseCase) {
    super(trueCase.type());
    this.condition = condition;
    this.c1 = c1;
    this.c2 = c2;
    this.trueCase = trueCase;
    this.falseCase = falseCase;
  }

  public ExpressionNode c1() {
    return c1;
  }

  public ExpressionNode c2() {
    return c2;
  }

  public TcgCondition condition() {
    return condition;
  }

  public ExpressionNode trueCase() {
    return trueCase;
  }

  public ExpressionNode falseCase() {
    return falseCase;
  }

  /**
   * The condition of the original select node was <b>not</b> inlined, iff the condition
   * EQ and the second condition operand is equal to true.
   *
   * @return true if the original {@link SelectNode} condition was inlined into this TCG condition,
   *     otherwise false.
   */
  public boolean inlined() {
    return !(condition == TcgCondition.EQ
        && c2 instanceof ConstantNode c
        && c.constant().asVal().equalValue(Constant.Value.of(true)));
  }

  /**
   * Produces the condition expression of this select node.
   * Based on the {@link #inlined()} check, this is either pure {@link #c1()} (as it was not
   * inlined), or an equal comparison between {@link #c1()} and {@link #c2()}.
   *
   * @return expression that might contain uninitialized nodes.
   */
  public ExpressionNode conditionExpr() {
    if (!inlined()) {
      return c1;
    }
    return TcgPassUtils.builtInOf(condition)
        .call(c1, c2);
  }

  @Override
  public ExpressionNode copy() {
    return new IssSelectNode(condition, c1.copy(), c2.copy(), trueCase.copy(), falseCase.copy());
  }

  @Override
  public Node shallowCopy() {
    return new IssSelectNode(condition, c1, c2, trueCase, falseCase);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(condition);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(c1);
    collection.add(c2);
    collection.add(trueCase);
    collection.add(falseCase);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    c1 = visitor.apply(this, c1, ExpressionNode.class);
    c2 = visitor.apply(this, c2, ExpressionNode.class);
    trueCase = visitor.apply(this, trueCase, ExpressionNode.class);
    falseCase = visitor.apply(this, falseCase, ExpressionNode.class);
  }
}
