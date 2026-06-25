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

package vadl.viam.graph.dependency;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * A sideeffect modifiying a stage, even if it is not directly returned by a stage.
 */
public class StageEffectNode extends SideEffectNode {

  @Input
  protected MiaBuiltInCall miaCall;

  public StageEffectNode(MiaBuiltInCall miaCall) {
    super(null);
    this.miaCall = miaCall;
  }

  StageEffectNode(MiaBuiltInCall miaCall, @Nullable ExpressionNode condition) {
    super(condition);
    this.miaCall = miaCall;
  }

  public MiaBuiltInCall miaCall() {
    return miaCall;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
  }

  @Override
  protected void forEachInput(Consumer<Node> consumer) {
    super.forEachInput(consumer);
    consumer.accept(miaCall);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    miaCall = visitor.apply(this, miaCall, MiaBuiltInCall.class);
  }

  @Override
  public Node copy() {
    return new StageEffectNode((MiaBuiltInCall) miaCall.copy(),
        (condition != null ? condition.copy() : null));
  }

  @Override
  public Node shallowCopy() {
    return new StageEffectNode(miaCall, condition);
  }

}
