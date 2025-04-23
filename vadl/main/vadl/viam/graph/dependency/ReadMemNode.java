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
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.Memory;
import vadl.viam.Resource;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * The ReadMemNode class is a concrete class that extends ReadNode.
 * It represents a node that reads a value from a memory location.
 */
public class ReadMemNode extends ReadResourceNode {

  @DataValue
  protected Memory memory;

  /**
   * Number of words read from memory.
   */
  @DataValue
  protected int words;

  /**
   * Constructs a ReadMemNode object with the specified memory, address, and data type.
   *
   * @param memory  the memory definition from which to read the value
   * @param words   the number of words that are read from address ({@code MEM<words>(addr)})
   * @param address the address expression node representing the address in memory to read from
   * @param type    the data type of the value being read
   */
  public ReadMemNode(Memory memory, int words, ExpressionNode address, DataType type) {
    super(address, type);
    this.memory = memory;
    this.words = words;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(memory.wordSize() * words == type().bitWidth(),
        "Type missmatch of expected node type and read bit width. %s vs %s",
        type().bitWidth(), memory.wordSize() * words);
  }

  public int words() {
    return words;
  }

  @Override
  public ExpressionNode address() {
    return indices().getFirst();
  }

  @Override
  public Resource resourceDefinition() {
    return memory;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(memory);
    collection.add(words);
  }

  @Override
  public ExpressionNode copy() {
    return new ReadMemNode(memory, words, (ExpressionNode) address().copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new ReadMemNode(memory, words, address(), type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  public Memory memory() {
    return memory;
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(memory.simpleName())
        .append("<").append(words).append(">(");
    address().prettyPrint(sb);
    sb.append(")");
  }
}
