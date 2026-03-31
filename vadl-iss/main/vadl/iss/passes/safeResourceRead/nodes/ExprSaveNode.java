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

package vadl.iss.passes.safeResourceRead.nodes;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an expression node that saves the value of another expression node.
 * <p>
 * This node is used to store the value of a resource read into a temporary location.
 * It is particularly useful in scenarios where resource reads might conflict with
 * subsequent writes, ensuring the original value is preserved for future use without
 * being affected by the write operations.
 * </p>
 */
public class ExprSaveNode extends ExpressionNode {

  /**
   * The expression node whose value is to be saved.
   */
  @Input
  private ExpressionNode value;

  /**
   * Constructs an {@code ExprSaveNode} with the given expression node.
   *
   * @param value The expression node whose value is to be saved.
   */
  public ExprSaveNode(ExpressionNode value) {
    super(value.type());
    this.value = value;
  }

  public ExpressionNode value() {
    return value;
  }

  @Override
  public ExpressionNode copy() {
    return new ExprSaveNode(value.copy(ExpressionNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new ExprSaveNode(value);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // not used
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
