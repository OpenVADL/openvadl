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

package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.ArrayList;
import java.util.List;
import vadl.error.Diagnostic;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionRegisterFileOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionFrameRegisterOperand;
import vadl.utils.Pair;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ReadRegTensorNode;

/**
 * Common superclass for {@link LlvmInstructionLoweringMemoryLoadStrategyImpl} and
 * {@link LlvmInstructionLoweringMemoryStoreStrategyImpl}.
 */
public abstract class LlvmInstructionLoweringFrameIndexHelper
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringFrameIndexHelper(
      ValueType architectureType, ValueType smallestRegisterClassType) {
    super(architectureType, smallestRegisterClassType);
  }

  @Override
  protected List<Pair<Graph, List<GcbInstructionOperand>>> deriveDifferentBehaviors(
      Instruction instruction,
      Graph copyBaseBehavior,
      List<GcbInstructionOperand> instructionInputOperands) {

    // Replace one register with a frame index node.
    var copy = copyBaseBehavior.copy();

    // We just replace the first occurrence and ignore the rest.
    var readRegNode = copy.getNodes(ReadRegTensorNode.class)
        .findFirst()
        .orElseThrow(() -> Diagnostic.error("Cannot find a read from a register file",
            instruction.location()).build());
    readRegNode.replaceAndDelete(new LlvmFrameIndexSD(readRegNode));

    var copyInputOperands = new ArrayList<>(instructionInputOperands);
    GcbInstructionRegisterFileOperand operand =
        (GcbInstructionRegisterFileOperand) copyInputOperands.stream()
            .filter(x -> x instanceof GcbInstructionRegisterFileOperand)
            .findFirst().orElseThrow(
                () -> Diagnostic.error("Expected at least one reference to a register file",
                    instruction.behavior().sourceLocation()).build());

    copyInputOperands.replaceAll(tableGenInstructionOperand -> {
      // If it was found then return other.
      if (tableGenInstructionOperand == operand) {
        return new TableGenInstructionFrameRegisterOperand(
            operand.origin(),
            operand.formatField().identifier);
      }

      return tableGenInstructionOperand;
    });

    return List.of(Pair.of(copy, copyInputOperands));
  }
}
