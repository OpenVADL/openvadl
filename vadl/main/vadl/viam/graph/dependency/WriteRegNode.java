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
import vadl.viam.Register;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.UniqueNode;

/**
 * Represents a write to register.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for register writes:
 * <li>A register may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteRegNode extends WriteResourceNode {

  @DataValue
  protected Register register;

  // a register-write might write to a counter.
  // if this is the case, the counter is set.
  @DataValue
  @Nullable
  private Counter.RegisterCounter staticCounterAccess;

  /**
   * Writes a value to a register node.
   *
   * @param register            the register node to write to
   * @param value               the value to write to the register
   * @param staticCounterAccess the {@link Counter} that is written,
   *                            or null if no counter is written
   */
  public WriteRegNode(Register register, ExpressionNode value,
                      @Nullable Counter.RegisterCounter staticCounterAccess) {
    super((ExpressionNode) null, value);
    this.register = register;
    this.staticCounterAccess = staticCounterAccess;
  }


  /**
   * Writes a value to a register node.
   *
   * @param register            the register node to write to
   * @param value               the value to write to the register
   * @param staticCounterAccess the {@link Counter} that is written,
   *                            or null if no counter is written
   * @param condition           the side condition of the node.
   */
  public WriteRegNode(Register register, ExpressionNode value,
                      @Nullable Counter.RegisterCounter staticCounterAccess,
                      @Nullable ExpressionNode condition) {
    super((ExpressionNode) null, value);
    this.register = register;
    this.staticCounterAccess = staticCounterAccess;
    this.condition = condition;
  }

  public Register register() {
    return register;
  }

  @Nullable
  public Counter.RegisterCounter staticCounterAccess() {
    return staticCounterAccess;
  }

  /**
   * Determines if the register is a PC based on whether staticCounterAccess is set.
   */
  public boolean isPcAccess() {
    return staticCounterAccess != null;
  }

  public void setStaticCounterAccess(@Nonnull Counter.RegisterCounter staticCounterAccess) {
    this.staticCounterAccess = staticCounterAccess;
  }

  @Override
  public Resource resourceDefinition() {
    return register;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(register);
    collection.add(staticCounterAccess);
  }

  @Override
  public Node copy() {
    return new WriteRegNode(register,
        (ExpressionNode) value.copy(),
        staticCounterAccess,
        (condition != null ? (ExpressionNode) condition.copy() : null));
  }

  @Override
  public Node shallowCopy() {
    return new WriteRegNode(register, value, staticCounterAccess, condition);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
