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

package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.BoolType;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents the If-Expression in a VADL specification.
 * All its cases produce a value and are side effect free.
 */
public class SelectNode extends ExpressionNode {

  @Input
  ExpressionNode condition;

  @Input
  ExpressionNode trueCase;
  @Input
  ExpressionNode falseCase;


  /**
   * Constructor to instantiate a select node.
   */
  public SelectNode(ExpressionNode condition, ExpressionNode trueCase, ExpressionNode falseCase) {
    this(trueCase.type(), condition, trueCase, falseCase);
  }

  /**
   * Constructor to instantiate a select node.
   */
  public SelectNode(Type type, ExpressionNode condition, ExpressionNode trueCase,
                    ExpressionNode falseCase) {
    super(type);
    this.condition = condition;
    this.trueCase = trueCase;
    this.falseCase = falseCase;

    ensure(trueCase.type().isTrivialCastTo(type),
        "True case can not be cast to result type. %s vs %s", trueCase.type(), type);
    ensure(falseCase.type().isTrivialCastTo(type),
        "False case can not be cast to result type. %s vs %s", falseCase.type(), type);
    ensure(condition.type().isTrivialCastTo(BoolType.bool()), "Condition must have type Bool");
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(condition);
    collection.add(trueCase);
    collection.add(falseCase);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.apply(this, condition, ExpressionNode.class);
    trueCase = visitor.apply(this, trueCase, ExpressionNode.class);
    falseCase = visitor.apply(this, falseCase, ExpressionNode.class);
  }

  @Override
  public String toString() {
    return "%s(%s, %s)".formatted(super.toString(), trueCase.id, falseCase.id);
  }

  @Override
  public ExpressionNode copy() {
    return new SelectNode(condition.copy(),
        trueCase.copy(),
        falseCase.copy());
  }

  @Override
  public Node shallowCopy() {
    return new SelectNode(condition, trueCase, falseCase);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  public ExpressionNode condition() {
    return condition;
  }

  public ExpressionNode trueCase() {
    return trueCase;
  }

  public ExpressionNode falseCase() {
    return falseCase;
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append("(");
    condition.prettyPrint(sb);
    sb.append(" ? ");
    trueCase.prettyPrint(sb);
    sb.append(" : ");
    falseCase.prettyPrint(sb);
    sb.append(")");
  }
}
