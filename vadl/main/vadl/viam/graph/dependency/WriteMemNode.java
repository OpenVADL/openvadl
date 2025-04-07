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
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.UniqueNode;

/**
 * Represents a write to memory.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for memory writes:
 * <li>A location may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteMemNode extends WriteResourceNode {

  @DataValue
  protected Memory memory;

  @DataValue
  protected int words;

  /**
   * Constructs a new WriteMemNode object.
   *
   * @param memory  the memory definition to write to
   * @param words   the number of words that are written to memory
   * @param address the expression representing the memory address
   * @param value   the expression representing the value to write
   */
  public WriteMemNode(Memory memory, int words, ExpressionNode address, ExpressionNode value) {
    super(address, value);
    this.memory = memory;
    this.words = words;
  }

  /**
   * Constructs a new WriteMemNode object.
   *
   * @param memory  the memory definition to write to
   * @param words   the number of words that are written to memory
   * @param address the expression representing the memory address
   * @param value   the expression representing the value to write
   */
  public WriteMemNode(Memory memory, int words, ExpressionNode address, ExpressionNode value,
                      @Nullable ExpressionNode condition) {
    super(address, value);
    this.memory = memory;
    this.words = words;
    this.condition = condition;
  }

  public Memory memory() {
    return memory;
  }

  public int words() {
    return words;
  }

  @Nonnull
  @Override
  public ExpressionNode address() {
    return super.address();
  }

  @Override
  public Resource resourceDefinition() {
    return memory;
  }

  @Override
  public int writeBitWidth() {
    return memory.wordSize() * words;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
    collection.add(words);
  }

  @Override
  public Node copy() {
    return new WriteMemNode(memory, words,
        (ExpressionNode) Objects.requireNonNull(address).copy(),
        (ExpressionNode) value.copy(),
        (condition != null ? condition.copy() : null));
  }

  @Override
  public Node shallowCopy() {
    return new WriteMemNode(memory, words, Objects.requireNonNull(address), value, condition);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
