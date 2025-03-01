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

package vadl.lcb.passes.llvmLowering.domain.machineDag;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Nodes in the machine graph for {@link TableGenPattern}.
 * Note that {@link PseudoInstruction} can also mimic an {@link Instruction}. Even
 * when they are not a machine instruction.
 */
public class LcbMachineInstructionParameterNode extends ExpressionNode {
  @DataValue
  private TableGenInstructionOperand instructionOperand;

  public LcbMachineInstructionParameterNode(TableGenInstructionOperand instructionOperand) {
    super(Type.dummy());
    this.instructionOperand = instructionOperand;
  }

  public TableGenInstructionOperand instructionOperand() {
    return instructionOperand;
  }

  public void setInstructionOperand(TableGenInstructionOperand operand) {
    this.instructionOperand = operand;
  }

  @Override
  public ExpressionNode copy() {
    return new LcbMachineInstructionParameterNode(instructionOperand);
  }

  @Override
  public Node shallowCopy() {
    return new LcbMachineInstructionParameterNode(instructionOperand);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((TableGenMachineInstructionVisitor) visitor).visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(instructionOperand);
  }
}
