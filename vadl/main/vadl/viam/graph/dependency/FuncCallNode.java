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
import vadl.viam.Function;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * A call to a function in a behaviour graph.
 *
 * <p>It has a list of arguments that must match the expected parameter by the given
 * {@link Function} definition.</p>
 */
public class FuncCallNode extends AbstractFunctionCallNode {

  @DataValue
  protected Function function;

  public FuncCallNode(Function function, NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.function = function;
  }


  // TODO: Remove this constructor
  @Deprecated
  public FuncCallNode(NodeList<ExpressionNode> args, Function function, Type type) {
    super(args, type);
    this.function = function;
  }


  public Function function() {
    return function;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    var params = function.parameters();
    var args = this.args;

    ensure(params.length == args.size(),
        "Number of arguments does not match number of parameters, %s vs %s", args.size(),
        params.length);

    for (int i = 0; i < args.size(); i++) {
      var arg = args.get(i);
      var param = params[i];
      ensure(param.type().isTrivialCastTo(arg.type()),
          "Argument does not match type of param %s, %s vs %s", param.simpleName(), param.type(),
          arg.type());
    }
    ensure(function.returnType().isTrivialCastTo(type()),
        "Return type of function does not match declared result type %s",
        type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(function);
  }

  @Override
  public ExpressionNode copy() {
    return new FuncCallNode(
        new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()),
        function, type());
  }

  @Override
  public Node shallowCopy() {
    return new FuncCallNode(arguments(), function, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(function.simpleName())
        .append("(");
    for (int i = 0; i < arguments().size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      arguments().get(i).prettyPrint(sb);
    }
    sb.append(")");
  }
}
