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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Node that represents matching the current instruction against a set of instructions.
 *
 * <p>Used to lower read/write conditions and select-by-instruction nodes. Conditions are extended
 * to include the output of an is-instruction node, select-by-instruction nodes are replaced with
 * selects that have is-instruction nodes as condition inputs. This moves the information present
 * in the IPG (i.e., which nodes are used for which instruction) to graph nodes before inlining the
 * IPG into the stages.
 */
public class IsInstructionNode extends ExpressionNode {

  @DataValue
  private final Set<Instruction> instructions;

  /**
   * Create a new is-instruction node for a set of instructions.
   *
   * @param instructions set of instructions
   */
  public IsInstructionNode(Set<Instruction> instructions) {
    super(Type.bool());
    this.instructions = new HashSet<>(instructions);
  }

  /**
   * Get the instructions this is-instruction node matches.
   *
   * @return set of instructions
   */
  public Set<Instruction> instructions() {
    return instructions;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(instructions);
  }

  @Override
  public ExpressionNode copy() {
    return new IsInstructionNode(instructions);
  }

  @Override
  public Node shallowCopy() {
    return new IsInstructionNode(instructions);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "(" + id + ") IsInstruction<Bool, " + instructions.size() + " instructions>";
  }
}
