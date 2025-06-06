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

package vadl.rtl.ipg.nodes;

import java.util.List;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * Represents a read from a register tensor on RTL. Compared to
 * {@link vadl.viam.graph.dependency.ReadRegTensorNode} this node has an additional condition input.
 */
public class RtlReadRegTensorNode extends ReadRegTensorNode implements RtlConditionalReadNode {

  @Input
  @Nullable
  protected ExpressionNode condition;

  /**
   * Constructs the node, which represents a read from a register tensor at some specific indices.
   *
   * @param regTensor           register to be read
   * @param indices             indices to accessed certain dimension
   * @param type                the type this node should be result in
   * @param condition           the read condition
   */
  public RtlReadRegTensorNode(RegisterTensor regTensor, NodeList<ExpressionNode> indices,
                              DataType type, @Nullable ExpressionNode condition,
                              @Nullable Counter staticCounterAccess) {
    super(regTensor, indices, type, staticCounterAccess);
    this.condition = condition;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.condition != null) {
      collection.add(condition);
    }
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.applyNullable(this, condition, ExpressionNode.class);
  }

  @Nullable
  @Override
  public ExpressionNode condition() {
    return condition;
  }

  /**
   * Sets the condition of the read.
   */
  @Override
  public void setCondition(ExpressionNode condition) {
    ensure(condition.type().isTrivialCastTo(Type.bool()), "Condition must be a boolean but was %s",
        condition);
    updateUsageOf(this.condition, condition);
    this.condition = condition;
  }

  @Override
  public ReadRegTensorNode copy() {
    return new RtlReadRegTensorNode(regTensor(), indices().copy(), type(),
        (condition == null) ? null : condition.copy(), staticCounterAccess());
  }

  @Override
  public ReadRegTensorNode shallowCopy() {
    return new RtlReadRegTensorNode(regTensor(), indices(), type(), condition,
        staticCounterAccess());
  }

  @Override
  public ReadResourceNode asReadNode() {
    return this;
  }
}
