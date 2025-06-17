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
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.dependency.ExpressionNode;

public class IssMoveNode extends DirectionalNode {

  @Input
  private ExpressionNode expr;

  @Input
  private TcgVRefNode dest;

  public IssMoveNode(TcgVRefNode dest, ExpressionNode expr) {
    this.expr = expr;
    this.dest = dest;
  }

  public IssMoveNode(TcgVRefNode dest, ExpressionNode expr, ControlNode next) {
    super(next);
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
    return new IssMoveNode(dest.copy(), expr.copy(), next().copy(ControlNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new IssMoveNode(dest, expr, next());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(expr);
    collection.add(dest);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    expr = visitor.apply(this, expr, ExpressionNode.class);
    dest = visitor.apply(this, dest, TcgVRefNode.class);
  }
}
