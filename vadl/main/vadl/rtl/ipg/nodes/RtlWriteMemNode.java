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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Represents a write to memory on RTL. Compared to
 * {@link vadl.viam.graph.dependency.WriteMemNode} this node has:
 * <li> Words input to enable merging all memory writes into one node with
 * the number of words written determined by this input
 * <li> Condition input
 */
public class RtlWriteMemNode extends WriteResourceNode {

  @DataValue
  protected Memory memory;

  @Input
  protected ExpressionNode words;

  /**
   * Construct new write memory node.
   *
   * @param memory memory to write to
   * @param words input with number of words to write
   * @param address address to write to
   * @param condition write condition
   */
  public RtlWriteMemNode(Memory memory, ExpressionNode words, ExpressionNode address,
                         ExpressionNode value, @Nullable ExpressionNode condition) {
    super(address, value);
    this.memory = memory;
    this.words = words;
    this.condition = condition;

    verifyState();
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
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    words = visitor.apply(this, words, ExpressionNode.class);
  }

  @Override
  public Resource resourceDefinition() {
    return memory;
  }

  public Memory memory() {
    return memory;
  }

  @Nonnull
  @Override
  public ExpressionNode address() {
    return super.address();
  }

  public ExpressionNode words() {
    return words;
  }

  @Override
  protected int writeBitWidth() {
    return -1;
  }

  @Override
  public Node copy() {
    return new RtlWriteMemNode(memory, words.copy(), address().copy(), value.copy(),
        (condition == null) ? null : condition.copy());
  }

  @Override
  public Node shallowCopy() {
    return new RtlWriteMemNode(memory, words, address(), value, condition);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
