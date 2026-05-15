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
import org.apache.commons.lang3.StringUtils;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.StructType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Extracts a field from a struct.
 *
 * <p>It stores the identifier as well as the label (once implemented)
 * to allow generating code with meaningful variable names.
 */
public class StructGetFieldNode extends ExpressionNode {

  @DataValue
  private final String field;

  @Input
  private ExpressionNode expression;

  /**
   * Constructs StructGetFieldNode.
   *
   * @param field      the field to get
   * @param expression the value that returns a struct
   */
  public StructGetFieldNode(String field, ExpressionNode expression, Type type) {
    super(type);
    this.expression = expression;
    this.field = field;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(StringUtils.isNotBlank(field), "Field is blank");

    if (!(expression.type() instanceof StructType structType)) {
      fail("The expression is not a struct, but %s", expression.type());
      return;
    }

    ensure(structType.get(field) != null, "The field %s does not exist in the struct.", field);
    ensure(structType.get(field).isTrivialCastTo(type()),
        "The node's type does not match the type retrieved from the expression.");
  }

  public String field() {
    return field;
  }

  public ExpressionNode expression() {
    return expression;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(field);
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
    return new StructGetFieldNode(field, expression.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new StructGetFieldNode(field, expression, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Replaces this node with its input, and then safely deletes this node.
   *
   * <p>The method ensures that the node to be deleted has usages, then it updates
   * the usages' input to bypass this node. Finally, it safely deletes this node
   * from the graph to maintain consistency.
   */
  public void replaceByNothingAndDelete() {
    var input = this.expression;
    usages().toList().forEach(usage -> usage.replaceInput(this, input));
    safeDelete();
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
