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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.gcb.valuetypes.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.immediates.GenerateTableGenImmediateRecordPass.ImmediateKey;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

/**
 * Lowers instructions into {@link TableGenInstruction}. This differs from
 * the default implementation because we want special flags in {@link #getFlags(Graph)}.
 */
public class LlvmInstructionLoweringXoriAndOriStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringXoriAndOriStrategyImpl(
      ValueType architectureType, ValueType smallestRegisterClassType,
      Map<ImmediateKey, TableGenImmediateRecord> tablegenImmediatesRecords) {
    super(architectureType, smallestRegisterClassType, tablegenImmediatesRecords);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(MachineInstructionLabel.XORI, MachineInstructionLabel.ORI);
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
      List<GcbInstructionOperand> inputOperands,
      List<GcbInstructionOperand> outputOperands,
      List<TableGenPattern> patterns,
      Abi abi) {
    return Collections.emptyList();
  }
}
