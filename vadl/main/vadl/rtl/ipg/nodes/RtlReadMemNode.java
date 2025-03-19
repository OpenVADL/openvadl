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
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ReadResourceNode;

/**
 * Represents a read from memory on RTL. Compared to
 * {@link vadl.viam.graph.dependency.ReadMemNode} this node differs in:
 * <li> Words input to enable merging all memory reads into one node with
 * the number of words read determined by this input
 * <li> Condition input
 */
public class RtlReadMemNode extends ReadResourceNode implements RtlConditionalReadNode {

  @DataValue
  protected Memory memory;

  @Input
  protected ExpressionNode words;

  @Input
  @Nullable
  protected ExpressionNode condition;

  /**
   * Construct new read memory node.
   *
   * @param memory memory to read from
   * @param words input with number of words to read
   * @param address address to read from
   * @param type read result type
   * @param condition read condition
   */
  public RtlReadMemNode(Memory memory, ExpressionNode words, ExpressionNode address,
                        DataType type, @Nullable ExpressionNode condition) {
    super(address, type);
    this.memory = memory;
    this.words = words;
    this.condition = condition;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(words);
    if (this.condition != null) {
      collection.add(condition);
    }
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    words = visitor.apply(this, words, ExpressionNode.class);
    condition = visitor.applyNullable(this, condition, ExpressionNode.class);
  }

  @Override
  public Resource resourceDefinition() {
    return memory;
  }

  public Memory memory() {
    return memory;
  }

  public ExpressionNode words() {
    return words;
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
  public ExpressionNode copy() {
    return new RtlReadMemNode(memory, words.copy(), address().copy(), type(),
        (condition != null) ? condition.copy() : null);
  }

  @Override
  public Node shallowCopy() {
    return new RtlReadMemNode(memory, words, address(), type(), condition);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public Node asNode() {
    return super.asNode();
  }
}
