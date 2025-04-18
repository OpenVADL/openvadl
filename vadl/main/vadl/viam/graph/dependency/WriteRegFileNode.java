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
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Counter;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.HasRegisterFile;
import vadl.viam.graph.Node;
import vadl.viam.graph.UniqueNode;

/**
 * Represents a write to register file.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for register writes:
 * <li>A register may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteRegFileNode extends WriteResourceNode implements HasRegisterFile {

  @DataValue
  protected RegisterFile registerFile;

  // a register-file-write might write to a counter.
  // if this is the case, the counter is set.
  // however, not all counter-accesses are statically known, as if the register file
  // is known, but the concrete index isn't,
  // it could be a counter written, but doesn't have to be.
  // it is generally set during the `StaticCounterAccessResolvingPass`
  @DataValue
  @Nullable
  private Counter.RegisterFileCounter staticCounterAccess;

  /**
   * Writes a value to a register file node.
   *
   * @param registerFile        The register file to write to.
   * @param address             The index/address node of the register file.
   * @param value               The value to be written.
   * @param staticCounterAccess the {@link Counter} that is written,
   *                            or null if it is not known
   */
  public WriteRegFileNode(RegisterFile registerFile, ExpressionNode address,
                          ExpressionNode value,
                          @Nullable Counter.RegisterFileCounter staticCounterAccess) {
    super(address, value);
    this.registerFile = registerFile;
    this.staticCounterAccess = staticCounterAccess;
  }

  /**
   * Writes a value to a register file node.
   *
   * @param registerFile        the register file to write to.
   * @param address             the index/address node of the register file.
   * @param value               the value to be written.
   * @param staticCounterAccess the {@link Counter} that is written,
   *                            or null if it is not known
   * @param condition           the node for the side effect.
   */
  public WriteRegFileNode(RegisterFile registerFile, ExpressionNode address,
                          ExpressionNode value,
                          @Nullable Counter.RegisterFileCounter staticCounterAccess,
                          @Nullable ExpressionNode condition) {
    super(address, value);
    this.registerFile = registerFile;
    this.staticCounterAccess = staticCounterAccess;
    this.condition = condition;
  }

  @Override
  public RegisterFile registerFile() {
    return registerFile;
  }

  @Nullable
  public Counter.RegisterFileCounter staticCounterAccess() {
    return staticCounterAccess;
  }

  public void setStaticCounterAccess(@Nonnull Counter.RegisterFileCounter staticCounterAccess) {
    this.staticCounterAccess = staticCounterAccess;
  }

  @Override
  public Resource resourceDefinition() {
    return registerFile;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(registerFile);
    collection.add(staticCounterAccess);
  }

  @Override
  @Nonnull
  public ExpressionNode address() {
    return Objects.requireNonNull(super.address());
  }

  @Override
  public Node copy() {
    return new WriteRegFileNode(registerFile,
        address().copy(),
        value.copy(),
        staticCounterAccess,
        (condition != null ? condition.copy() : null));
  }

  @Override
  public Node shallowCopy() {
    return new WriteRegFileNode(registerFile,
        address(),
        value,
        staticCounterAccess,
        condition);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
