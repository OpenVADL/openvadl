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
import vadl.utils.SourceLocation;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a let expression in the VADL Specification.
 *
 * <p>It stores the identifier as well as the label (once implemented)
 * to allow generating code with meaningful variable names.
 */
public class LetNode extends ExpressionNode {
  // TODO: Add label functionality

  @DataValue
  protected Name name;

  @Input
  protected ExpressionNode expression;

  /**
   * Constructs a let-node.
   *
   * @param name       the name of the let assignment
   * @param expression the value of the let assignment
   */
  public LetNode(Name name, ExpressionNode expression) {
    super(expression.type());
    this.name = name;
    this.expression = expression;
  }

  public Name letName() {
    return name;
  }

  public ExpressionNode expression() {
    return expression;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(name);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(expression);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    expression = visitor.apply(this, expression, ExpressionNode.class);
  }

  @Override
  public ExpressionNode copy() {
    return new LetNode(name, (ExpressionNode) expression.copy());
  }

  @Override
  public Node shallowCopy() {
    return new LetNode(name, expression);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    expression.prettyPrint(sb);
  }


  /**
   * The name of a let expression with source location.
   */
  public record Name(
      String name,
      SourceLocation location
  ) {

    @Override
    public String toString() {
      return name;
    }
  }
}
