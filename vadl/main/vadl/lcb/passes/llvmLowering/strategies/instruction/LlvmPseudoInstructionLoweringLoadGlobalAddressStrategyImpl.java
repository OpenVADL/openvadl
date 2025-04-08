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
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstAlias;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Whereas {@link LlvmInstructionLoweringStrategy} defines multiple to lower {@link Instruction}
 * a.k.a Machine Instructions, this class lowers {@link PseudoInstruction} when the instruction
 * is loading a global address. The reason is that this pseudo instruction requires the
 * {@code mayLoad} flag.
 */
public class LlvmPseudoInstructionLoweringLoadGlobalAddressStrategyImpl
    extends LlvmPseudoInstructionLowerStrategy {

  private final Abi abi;

  public LlvmPseudoInstructionLoweringLoadGlobalAddressStrategyImpl(
      List<LlvmInstructionLoweringStrategy> strategies,
      Abi abi) {
    super(strategies);
    this.abi = abi;
  }

  @Override
  protected Set<PseudoInstructionLabel> getSupportedInstructionLabels() {
    return Set.of();
  }

  @Override
  public boolean isApplicable(@Nullable PseudoInstructionLabel pseudoInstructionLabel,
                              PseudoInstruction pseudoInstruction) {
    return abi.picAddressLoad().isPresent() && abi.picAddressLoad().get() == pseudoInstruction;
  }

  @Override
  public Optional<LlvmLoweringRecord.Pseudo> lowerInstruction(Abi abi,
                                                              List<TableGenInstAlias> instAliases,
                                                              PseudoInstruction pseudo,
                                                              IsaMachineInstructionMatchingPass.Result supportedInstructions) {
    var record = super.lowerInstruction(abi, instAliases, pseudo, supportedInstructions);

    if (record.isPresent()) {
      // The pseudo instruction which loads the global address needs to have the
      // `mayLoad` flag.
      var result = record.get().info()
          .withFlags(LlvmLoweringPass.Flags.withMayLoad(record.get().info().flags()));
      return Optional.of((LlvmLoweringRecord.Pseudo) record.get().withInfo(result));
    }

    return record;
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(PseudoInstruction pseudo,
                                                            LlvmLoweringRecord record) {
    return Collections.emptyList();
  }
}
