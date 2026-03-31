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
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a node with side effect. Such nodes are dependencies
 * of {@link vadl.viam.graph.control.AbstractEndNode}.
 *
 * <p>A side effect has a {@code condition} under which it takes affect/is executed.
 * This condition is resolved during the
 * {@link vadl.viam.passes.sideeffect_condition.SideEffectConditionResolvingPass} and
 * therefore is not available until the pass was executed.</p>
 */
public abstract class SideEffectNode extends DependencyNode {

  @Input
  @Nullable
  protected ExpressionNode condition;

  public SideEffectNode(@Nullable ExpressionNode condition) {
    this.condition = condition;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    if (condition != null) {
      ensure(condition.type().isTrivialCastTo(Type.bool()),
          "Condition must be a boolean but was %s",
          condition);
    }
  }

  public ExpressionNode condition() {
    ensure(condition != null, "Condition was expected to be not null.");
    return condition;
  }

  public @Nullable ExpressionNode nullableCondition() {
    return condition;
  }

  /**
   * Sets the condition of the side effect.
   * The condition defines under what condition the side effect takes place.
   */
  public void setCondition(@Nullable ExpressionNode condition) {
    if (condition != null) {
      ensure(condition.type().isTrivialCastTo(Type.bool()),
          "Condition must be a boolean but was %s",
          condition);
    }
    updateUsageOf(this.condition, condition);
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
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.applyNullable(this, condition, ExpressionNode.class);
  }
}
