// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.function.Consumer;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a move operation of an expression to a {@link TcgVRefNode}.
 * This is used by the {@link vadl.iss.passes.IssSelectLoweringPass} to provide an expression
 * while the branch condition is selected using a control flow.
 */
public class IssMoveNode extends DependencyNode {
  @Input
  private TcgVRefNode dest;

  @Input
  private ExpressionNode expr;


  public IssMoveNode(TcgVRefNode dest, ExpressionNode expr) {
    this.expr = expr;
    this.dest = dest;
  }

  public ExpressionNode expr() {
    return expr;
  }

  public TcgVRefNode dest() {
    return dest;
  }

  @Override
  public Node copy() {
    return new IssMoveNode(dest.copy(), expr.copy());
  }

  @Override
  public Node shallowCopy() {
    return new IssMoveNode(dest, expr);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void forEachInput(Consumer<Node> consumer) {
    super.forEachInput(consumer);
    consumer.accept(dest);
    consumer.accept(expr);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    dest = visitor.apply(this, dest, TcgVRefNode.class);
    expr = visitor.apply(this, expr, ExpressionNode.class);
  }
}

