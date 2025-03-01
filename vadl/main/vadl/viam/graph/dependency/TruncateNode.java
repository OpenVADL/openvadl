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

import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a truncation of the node's value the assigned type.
 * This node is constructed during the
 * {@link vadl.viam.passes.typeCastElimination.TypeCastEliminationPass}.
 *
 * @see vadl.viam.passes.typeCastElimination.TypeCastEliminator
 */
public class TruncateNode extends UnaryNode implements Canonicalizable {

  public TruncateNode(ExpressionNode value, DataType type) {
    super(value, type);
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(super.type() instanceof DataType, "Type must be a data type");
    ensure(value.type() instanceof DataType, "Value must be a data type");
    ensure(((DataType) value.type()).bitWidth() >= type().bitWidth(),
        "Value's type bit-width must be greater or equal node's type");
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append("trunc<").append(type().bitWidth()).append(">(");
    value.prettyPrint(sb);
    sb.append(")");
  }

  @Override
  public ExpressionNode copy() {
    return new TruncateNode((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new TruncateNode(value, type());
  }

  @Override
  public Node canonical() {
    if (value instanceof ConstantNode constantNode
        && constantNode.constant instanceof Constant.Value constant) {
      // if the constant node we can zero extend the node
      return new ConstantNode(constant.truncate(this.type()));
    }
    return this;
  }
}
