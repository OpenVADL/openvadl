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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstAlias;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Abi;
import vadl.viam.PseudoInstruction;

/**
 * Defines a {@link PseudoInstruction} will be lowered to {@link TableGenInstruction}.
 */
public abstract class LlvmPseudoInstructionLowerStrategy
    extends LlvmCompilerInstructionLowerStrategy {
  /**
   * Constructor.
   */
  protected LlvmPseudoInstructionLowerStrategy(List<LlvmInstructionLoweringStrategy> strategies) {
    super(strategies);
  }

  /**
   * Get the supported set of {@link PseudoInstructionLabel} which this strategy supports.
   */
  protected abstract Set<PseudoInstructionLabel> getSupportedInstructionLabels();

  /**
   * Checks whether the given {@link PseudoInstruction} is lowerable with this strategy.
   */
  public boolean isApplicable(@Nullable PseudoInstructionLabel pseudoInstructionLabel) {
    if (pseudoInstructionLabel == null) {
      return false;
    }

    return getSupportedInstructionLabels().contains(pseudoInstructionLabel);
  }

  /**
   * Lower a {@link PseudoInstruction} into a {@link LlvmLoweringRecord.Pseudo}.
   */
  public Optional<LlvmLoweringRecord.Pseudo> lowerInstruction(
      Abi abi,
      List<TableGenInstAlias> instAliases,
      PseudoInstruction pseudo,
      IsaMachineInstructionMatchingPass.Result supportedInstructions) {
    var compilerInstruction = super.lowerInstruction(pseudo, supportedInstructions);

    return compilerInstruction.map(x -> {
      var flags = LlvmLoweringPass.Flags.withNoCodeGenOnly(x.info().flags());
      return new LlvmLoweringRecord.Pseudo(
          x.info().withFlags(flags),
          x.patterns(),
          instAliases
      );
    });
  }

  protected List<TableGenPattern> generatePatternVariations(
      PseudoInstruction pseudo,
      LlvmLoweringRecord record) {
    return Collections.emptyList();
  }
}
