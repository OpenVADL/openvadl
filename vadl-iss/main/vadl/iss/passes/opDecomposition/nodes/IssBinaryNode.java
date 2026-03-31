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

package vadl.iss.passes.opDecomposition.nodes;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * An ISS expression with exactly two arguments.
 */
public abstract class IssBinaryNode extends IssExprNode {

  @Input
  private ExpressionNode arg1;

  @Input
  private ExpressionNode arg2;

  /**
   * The constructor of the binary ISS expression. It takes two arguments and a type of the result.
   */
  public IssBinaryNode(ExpressionNode arg1, ExpressionNode arg2, Type type) {
    super(type);
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  public ExpressionNode arg1() {
    return arg1;
  }

  public ExpressionNode arg2() {
    return arg2;
  }

  @Override
  public Type type() {
    return super.type();
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg1 = visitor.apply(this, arg1, ExpressionNode.class);
    arg2 = visitor.apply(this, arg2, ExpressionNode.class);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg1);
    collection.add(arg2);
  }
}
