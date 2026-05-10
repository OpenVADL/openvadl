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

import javax.annotation.Nullable;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Represents writing to a register tensor on RTL. Compared to
 * {@link vadl.viam.graph.dependency.WriteRegTensorNode} this node has:
 * <li>?
 */
public class RtlWriteRegTensorNode extends WriteRegTensorNode implements RtlConditionalNode {

  /**
   * Construct a new write register tensor node.
   *
   * @param regTensor           register to be written
   * @param indices             index that is written
   * @param value               the value that is written
   * @param staticCounterAccess if this writes to a counter-register, this might be non-null
   * @param condition           condition for write
   */
  public RtlWriteRegTensorNode(RegisterTensor regTensor,
                               NodeList<ExpressionNode> indices, ExpressionNode value,
                               @Nullable Counter staticCounterAccess,
                               @Nullable ExpressionNode condition) {
    super(regTensor, indices, value, staticCounterAccess, condition);
  }

  @Override
  public RtlWriteRegTensorNode copy() {
    return new RtlWriteRegTensorNode(regTensor, indices.copy(), value.copy(), staticCounterAccess(),
        (condition == null) ? null : condition.copy());
  }

  @Override
  public RtlWriteRegTensorNode shallowCopy() {
    return new RtlWriteRegTensorNode(regTensor, indices, value, staticCounterAccess(), condition);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public Node asNode() {
    return this;
  }
}
