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
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;


/**
 * Represents a unary node in the VIAM graph.
 * A unary node is an expression node with only one input node.
 */
// TODO: Check if we should remove this
public abstract class UnaryNode extends ExpressionNode {

  @Input
  protected ExpressionNode value;

  public UnaryNode(ExpressionNode value, Type type) {
    super(type);
    this.value = value;
  }

  /**
   * Return the value of the node.
   */
  public ExpressionNode value() {
    return this.value;
  }


  /**
   * Will remap the edges between usages and inputs in a way that this node isn't used
   * anymore.
   *
   * <p>Visually this can be represented as<pre>
   *            Input                           Input
   *              |                               |
   *              |                              /|\
   *          This Node       --------->        / | \
   *           /  |  \                         /  |  \
   *          /   |   \                       /   |   \
   *    User-1   ...   User-N           User-1   ...   User-N*
   *
   * </pre>
   */
  public void replaceByNothingAndDelete() {
    ensure(!isDeleted(), "Replacing a deleted node is not allowed.");

    // direct user's inputs to the input of this
    // and set the users as usages of this value/input.
    for (var user : usages().toList()) {
      user.replaceInput(this, value);
    }
    // remove this node from the list of usages of value
    value.removeUsage(this);
    // delete this node
    safeDelete();
  }


  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }
}
