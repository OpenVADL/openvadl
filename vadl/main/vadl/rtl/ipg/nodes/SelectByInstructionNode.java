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

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Node that implements a select based on the instruction currently executed.
 */
public class SelectByInstructionNode extends ExpressionNode {

  @DataValue
  List<Set<Instruction>> instructions;

  @Input
  NodeList<ExpressionNode> values;

  /**
   * Create new select by instruction node. Add instructions and associated values later
   * (see {@link SelectByInstructionNode#add}).
   *
   * @param type result type
   */
  public SelectByInstructionNode(Type type) {
    this(type, new ArrayList<>(), new NodeList<>());
  }

  /**
   * Create new select by instruction node with a list of sets of instructions to select by and
   * a list of values, which the output of this node is selected from.
   * The instructions and values lists need to have the same length.
   *
   * @param type result type
   * @param instructions list of sets of instructions
   * @param values list of values for the result
   */
  public SelectByInstructionNode(Type type, List<Set<Instruction>> instructions,
                                 NodeList<ExpressionNode> values) {
    super(type);
    ensure(instructions.size() == values.size(),
        "List of instruction sets must have same size as value inputs");
    this.instructions = instructions;
    this.values = values;
  }

  /**
   * Add a pair of an instruction and a value to this select node. The output of this node will be
   * the given value, if the instruction matches.
   *
   * @param instruction instruction
   * @param value value
   */
  public void add(Instruction instruction, ExpressionNode value) {
    for (int i = 0; i < instructions.size(); i++) {
      if (instructions.get(i).contains(instruction)) {
        ensure(values.get(i).equals(value),
            "Can not add instruction multiple times with different values");
        return; // instruction and value was already added
      }
    }
    for (int i = 0; i < values.size(); i++) {
      if (values.get(i).equals(value)) {
        instructions.get(i).add(instruction); // add to existing set of instructions
        return;
      }
    }
    var set = new HashSet<Instruction>();
    set.add(instruction);
    instructions.add(set);
    values.add(value);
    if (isActive()) {
      updateUsageOf(null, value);
    }
  }

  /**
   * Add instructions and values of another select node to this node.
   *
   * @param other the other select node
   */
  public void merge(SelectByInstructionNode other) {
    Streams.forEachPair(
        other.instructions.stream(),
        other.values.stream(),
        (set, value) -> {
          for (Instruction instruction : set) {
            add(instruction, value);
          }
        });
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(instructions);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(values);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    values = values.stream()
        .map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  public ExpressionNode copy() {
    return new SelectByInstructionNode(type(), instructions, values.copy());
  }

  @Override
  public Node shallowCopy() {
    return new SelectByInstructionNode(type(), instructions, values);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    var data = "<%s, %d inputs>".formatted(type().toString(), values.size());
    return "(%s) %s%s".formatted(id, nodeName(), data);
  }
}
