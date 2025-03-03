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
import vadl.viam.Definition;
import vadl.viam.Parameter;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a parameter node for a function in VADL specification.
 *
 * <p>This node does only exist in graphs that belong to functions.
 */
public class FuncParamNode extends ParamNode {

  @DataValue
  protected Parameter parameter;

  /**
   * Constructs a FuncParamNode instance with a given parameter and type.
   * The node type and parameter type must be equal.
   */
  public FuncParamNode(Parameter parameter) {
    super(parameter.type());
    this.parameter = parameter;
  }

  public Parameter parameter() {
    return parameter;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(parameter);
  }

  @Override
  public ExpressionNode copy() {
    return new FuncParamNode(parameter);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Definition definition() {
    return parameter;
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(parameter.simpleName());
  }
}
