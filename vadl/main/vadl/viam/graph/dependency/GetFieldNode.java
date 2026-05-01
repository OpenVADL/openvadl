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
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * An expression node representing a field access, e.g. on an index of the
 * forall-then expression.
 * <pre>
 *   forall x in {O1, O2} then x.a
 * </pre>
 */
public class GetFieldNode extends ExpressionNode {

  @DataValue
  private final String fieldName;

  @Input
  private ExpressionNode expression;

  /**
   * The constructor.
   *
   * @param fieldName  name of the field.
   * @param expression the target expression.
   * @param type       the field type.
   */
  public GetFieldNode(String fieldName, ExpressionNode expression, Type type) {
    super(type);

    this.expression = expression;
    this.fieldName = fieldName;
  }

  public String fieldName() {
    return fieldName;
  }

  public ExpressionNode expression() {
    return expression;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(fieldName);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(expression);
  }

  @Override
  public ExpressionNode copy() {
    return new GetFieldNode(fieldName, expression.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new GetFieldNode(fieldName, expression, type());
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    expression = visitor.apply(this, expression, ExpressionNode.class);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
