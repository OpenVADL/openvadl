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
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * An abstract function call node that has a list of arguments as input.
 * A concrete subtype is the {@link BuiltInCall}.
 */
public abstract class AbstractFunctionCallNode extends ExpressionNode {

  @Input
  protected NodeList<ExpressionNode> args;

  public AbstractFunctionCallNode(NodeList<ExpressionNode> args, Type type) {
    super(type);
    this.args = args;
  }

  public NodeList<ExpressionNode> arguments() {
    return args;
  }

  public void setArgs(NodeList<ExpressionNode> args) {
    this.args = args;
  }

  /**
   * Checks whether all the inputs of the node are constant.
   *
   * @return {@code true} if all the inputs are {@link ConstantNode} and {@code false}
   *     if any is not {@link ConstantNode}.
   */
  protected boolean hasConstantArgs() {
    return inputs().allMatch(x -> x instanceof ConstantNode);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(args);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    args = args.stream()
        .map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}