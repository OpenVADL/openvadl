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

import static vadl.gcb.passes.MachineInstructionLabel.ADDI_32;
import static vadl.gcb.passes.MachineInstructionLabel.ADDI_64;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Lowers add with immediate into {@link TableGenInstruction} and additionally,
 * creates alternative patterns with {@link LlvmFrameIndexSD}.
 */
public class LlvmInstructionLoweringAddImmediateStrategyImpl
    extends LlvmInstructionLoweringFrameIndexHelper {
  private final Set<MachineInstructionLabel> supported = Set.of(ADDI_32, ADDI_64);

  public LlvmInstructionLoweringAddImmediateStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return supported;
  }

  @Override
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithDefaultFieldAccessReplacement();
  }

  @Override
  protected LlvmLoweringPass.Flags getFlags(Graph graph) {
    var flags = super.getFlags(graph);

    return LlvmLoweringPass.Flags.withIsRematerialisable(
        LlvmLoweringPass.Flags.withIsAsCheapAsMove(flags));
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      IsaMachineInstructionMatchingPass.Result supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      Abi abi) {
    var alternativePatterns = new ArrayList<TableGenPattern>();

    // We are only interested in the pattern with selector and machine pattern.
    patterns.stream()
        .filter(p -> p instanceof TableGenSelectionWithOutputPattern)
        .map(p -> (TableGenSelectionWithOutputPattern) p)
        .forEach(pattern -> {
          var selector = pattern.selector().copy();
          var machine = pattern.machine().copy();

          var affectedNodes = selector.getNodes(LlvmReadRegFileNode.class).toList();
          // Only add a new pattern when something is affected.
          // Otherwise, we get a duplicated pattern.
          if (!affectedNodes.isEmpty()) {
            alternativePatterns.add(
                super.replaceRegisterWithFrameIndex(selector, machine, affectedNodes));
          }
        });

    return alternativePatterns;
  }
}
