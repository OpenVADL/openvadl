// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.graph;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Node which indicates that a field or field access function is used. This is required for the LCB
 * since not all operands of an instruction must be part of a behavior.
 */
public class DefinedImmediateSideEffectNode extends SideEffectNode {
  @Input
  private ExpressionNode value;

  public DefinedImmediateSideEffectNode(@Nullable ExpressionNode condition, ExpressionNode value) {
    super(condition);
    this.value = value;
  }

  @Override
  public Node copy() {
    return new DefinedImmediateSideEffectNode(condition != null
        ? condition.copy() : null,
        value.copy());
  }

  @Override
  public Node shallowCopy() {
    return new DefinedImmediateSideEffectNode(condition, value);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  protected void forEachInput(Consumer<Node> consumer) {
    super.forEachInput(consumer);
    consumer.accept(value);
  }

  @Override
  public void applyOnInputsUnsafe(
      vadl.viam.graph.GraphVisitor.Applier<vadl.viam.graph.Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }
}
