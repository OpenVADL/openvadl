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
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Whereas {@link LlvmInstructionLoweringStrategy} defines multiple to lower {@link Instruction}
 * a.k.a Machine Instructions, this class lowers {@link PseudoInstruction}. But only as a fallback
 * strategy when no other strategy is applicable.
 */
public class LlvmPseudoInstructionLoweringDefaultStrategyImpl
    extends LlvmPseudoInstructionLowerStrategy {
  public LlvmPseudoInstructionLoweringDefaultStrategyImpl(
      List<LlvmInstructionLoweringStrategy> strategies) {
    super(strategies);
  }

  @Override
  protected Set<PseudoInstructionLabel> getSupportedInstructionLabels() {
    return Set.of();
  }

  @Override
  public boolean isApplicable(@Nullable PseudoInstructionLabel pseudoInstructionLabel) {
    // This is strategy should be always applicable.
    return true;
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(PseudoInstruction pseudo,
                                                            LlvmLoweringRecord record) {
    return Collections.emptyList();
  }

  @Override
  protected void updatePatterns(PseudoInstruction pseudo, LlvmLoweringRecord record) {

  }
}
