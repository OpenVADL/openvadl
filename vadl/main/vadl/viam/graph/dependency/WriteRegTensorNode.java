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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Counter;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Writes to a {@link RegisterTensor}.
 * TODO: Add more documentation
 */
public class WriteRegTensorNode extends WriteResourceNode {

  @DataValue
  protected RegisterTensor regTensor;

  // a register-write might write to a counter.
  // if this is the case, the counter is set.
  // however, not all counter-accesses are statically known, as if the register file
  // is known, but the concrete index isn't,
  // it could be a counter written, but doesn't have to be.
  // it is generally set during the `StaticCounterAccessResolvingPass`
  @DataValue
  @Nullable
  private Counter staticCounterAccess;

  /**
   * Construct the {@link WriteRegTensorNode}.
   *
   * @param regTensor           register to be written
   * @param indices             index that is written
   *                            (start outermost dimension, end innermost dimension)
   * @param value               the value that is written
   * @param staticCounterAccess if this writes to a counter-register, this might be non-null
   */
  public WriteRegTensorNode(RegisterTensor regTensor, NodeList<ExpressionNode> indices,
                            ExpressionNode value, @Nullable Counter staticCounterAccess) {
    super(indices, value);
    this.regTensor = regTensor;
    this.staticCounterAccess = staticCounterAccess;
  }

  @Override
  public RegisterTensor resourceDefinition() {
    return regTensor;
  }

  /**
   * Determines if the register is a PC based on whether staticCounterAccess is set.
   */
  public boolean isPcAccess() {
    return staticCounterAccess != null;
  }

  @Nullable
  public Counter staticCounterAccess() {
    return staticCounterAccess;
  }

  /**
   * This is set by the
   * {@link vadl.viam.passes.staticCounterAccess.StaticCounterAccessResolvingPass}.
   *
   * @param staticCounterAccess the counter that is accessed.
   * @see vadl.viam.passes.staticCounterAccess.StaticCounterAccessResolvingPass
   */
  public void setStaticCounterAccess(@Nonnull Counter staticCounterAccess) {
    this.staticCounterAccess = staticCounterAccess;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(indices().size() <= regTensor.maxNumberOfAccessIndices(),
        "Too many indices for tensor access. Write uses %d indices, tensor has %d indices",
        indices().size(), regTensor.maxNumberOfAccessIndices());
    // TODO: Check index types
  }

  @Override
  public Node copy() {
    return new WriteRegTensorNode(regTensor, indices.copy(), value.copy(), staticCounterAccess());
  }

  @Override
  public Node shallowCopy() {
    return new WriteRegTensorNode(regTensor, indices, value, staticCounterAccess());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(regTensor);
    collection.add(staticCounterAccess);
  }
}
