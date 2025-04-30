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

package vadl.lcb.passes.llvmLowering.strategies;

import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionLabelOperand;

/**
 * Utilities for lowering.
 */
public class LoweringStrategyUtils {

  /**
   * Conditional and unconditional branch patterns reference the {@code bb} selection dag node.
   * However, the machine instruction should use the label immediate to properly encode the
   * instruction.
   */
  public static TableGenPattern replaceBasicBlockByLabelImmediateInMachineInstruction(
      TableGenPattern pattern) {

    if (pattern instanceof TableGenSelectionWithOutputPattern) {
      // We know that the `selector` already has LlvmBasicBlock nodes.
      var candidates = ((TableGenSelectionWithOutputPattern) pattern).machine().getNodes(
          LcbMachineInstructionParameterNode.class).toList();
      for (var candidate : candidates) {
        if (candidate.instructionOperand().origin() instanceof LlvmBasicBlockSD basicBlockSD) {
          candidate.setInstructionOperand(
              new TableGenInstructionLabelOperand(basicBlockSD));
        }
      }
    }

    return pattern;
  }
}
