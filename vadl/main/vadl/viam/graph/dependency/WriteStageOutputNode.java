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
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.StageOutput;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Write value from expression node to stage output.
 */
public class WriteStageOutputNode extends SideEffectNode {

  @DataValue
  protected StageOutput stageOutput;

  @Input
  protected ExpressionNode value;

  /**
   * Construct a new WriteStageOutputNode.
   *
   * @param stageOutput stage output that is written
   * @param value value to be written
   */

  public WriteStageOutputNode(StageOutput stageOutput, ExpressionNode value) {
    this.stageOutput = stageOutput;
    this.value = value;
  }

  /**
   * Construct a new WriteStageOutputNode.
   *
   * @param stageOutput stage output that is written
   * @param value value to be written
   * @param condition write enable condition
   */
  public WriteStageOutputNode(StageOutput stageOutput, ExpressionNode value,
                              @Nullable ExpressionNode condition) {
    this.stageOutput = stageOutput;
    this.value = value;
    this.condition = condition;
  }

  public StageOutput stageOutput() {
    return stageOutput;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(stageOutput);
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

  @Override
  public Node copy() {
    return new WriteStageOutputNode(stageOutput,
        value.copy(),
        (condition != null ? condition.copy() : null));
  }

  @Override
  public Node shallowCopy() {
    return new WriteStageOutputNode(stageOutput, value, condition);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
