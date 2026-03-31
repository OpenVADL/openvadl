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
import vadl.types.Type;

/**
 * Expression nodes produce some value and therefore also
 * hold the type of the value. This is required to maintain
 * graph consistency during graph transformation.
 */
public abstract class ExpressionNode extends DependencyNode {

  @DataValue
  private Type type;

  public ExpressionNode(Type type) {
    this.type = type;
  }

  //  public abstract Type type();
  public Type type() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public boolean isConstant() {
    return this instanceof ConstantNode;
  }

  /**
   * Overrides the type of the node.
   */
  public void overrideType(Type type) {
    this.type = type;
  }

  @Override
  public abstract ExpressionNode copy();

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(type);
  }

  /**
   * Creates a pretty-printed representation of the current expression node.
   *
   * @return A string containing the pretty-printed representation of the expression node.
   */
  public final String prettyPrint() {
    StringBuilder sb = new StringBuilder();
    prettyPrint(sb);
    return sb.toString();
  }

  /**
   * Appends a pretty-printed representation of the current expression node to the
   * provided StringBuilder.
   *
   * @param sb The StringBuilder to which the pretty-printed representation is appended.
   */
  public void prettyPrint(StringBuilder sb) {
    sb.append("prettyPrint(").append(getClass().getSimpleName()).append(")");
  }

}
