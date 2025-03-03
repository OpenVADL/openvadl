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
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * The constant node represents a compile time constant value in the
 * data dependency graph.
 */
public class ConstantNode extends ExpressionNode {

  @DataValue
  protected Constant constant;

  public ConstantNode(Constant constant) {
    super(constant.type());
    this.constant = constant;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(constant);
  }

  /**
   * Set the {@link Constant}.
   */
  public void setConstant(Constant constant) {
    this.constant = constant;
  }

  /**
   * Return the {@link Constant}.
   */
  public Constant constant() {
    return this.constant;
  }

  @Override
  public ExpressionNode copy() {
    return new ConstantNode(constant);
  }

  @Override
  public Node shallowCopy() {
    return new ConstantNode(constant);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb
        .append("(")
        .append(constant.toString())
        .append(")")
    ;
  }
}
