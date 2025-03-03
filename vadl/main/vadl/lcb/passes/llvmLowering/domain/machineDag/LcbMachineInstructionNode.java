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
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.AbstractFunctionCallNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an {@link Instruction} in a {@link Graph}.
 */
public class LcbMachineInstructionNode extends AbstractFunctionCallNode {
  @DataValue
  protected OutputInstructionName outputInstructionName;

  /**
   * Constructor.
   */
  public LcbMachineInstructionNode(NodeList<ExpressionNode> args,
                                   Instruction instruction) {
    super(args,
        Type.dummy());
    this.outputInstructionName = new OutputInstructionName(instruction.identifier.simpleName());
  }

  /**
   * Constructor.
   */
  public LcbMachineInstructionNode(NodeList<ExpressionNode> args,
                                   OutputInstructionName outputInstructionName) {
    super(args,
        Type.dummy());
    this.outputInstructionName = outputInstructionName;
  }

  @Override
  public ExpressionNode copy() {
    return new LcbMachineInstructionNode(
        new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        outputInstructionName);
  }

  @Override
  public Node shallowCopy() {
    return new LcbMachineInstructionNode(args, outputInstructionName);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    ((TableGenMachineInstructionVisitor) visitor).visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(outputInstructionName);
  }

  public OutputInstructionName outputInstructionName() {
    return outputInstructionName;
  }

  public void setOutputInstruction(Instruction instruction) {
    this.outputInstructionName = new OutputInstructionName(instruction.identifier.simpleName());
  }
}
