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
import vadl.types.TupleType;
import vadl.types.Type;
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
public class TupleGetFieldNode extends ExpressionNode {

  @DataValue
  private int index;

  @Input
  private ExpressionNode expression;

  /**
   * Constructs TupleGetFieldNode.
   *
   * @param index      the index to get
   * @param expression the value that returns a tuple
   */
  public TupleGetFieldNode(int index, ExpressionNode expression, Type type) {
    super(type);
    this.expression = expression;
    this.index = index;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(index >= 0, "Index is negative.");
    ensure(expression.type() instanceof TupleType, "The expression result not in tuple, but in %s",
        expression.type());
    ensure(index < ((TupleType) expression.type()).size(),
        "The index of is out of bound. i: %s, tuple: %s", index, expression.type());
    ensure(((TupleType) expression.type()).get(index).isTrivialCastTo(type()),
        "The node's type does not match the type retrieved from the expression.");
  }

  public int index() {
    return index;
  }

  public ExpressionNode expression() {
    return expression;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(index);
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
    return new TupleGetFieldNode(index, (ExpressionNode) expression.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new TupleGetFieldNode(index, expression, type());
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
