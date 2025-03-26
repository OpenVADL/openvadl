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

import java.util.List;
import vadl.lcb.passes.llvmLowering.strategies.LlvmCompilerInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.viam.CompilerInstruction;
import vadl.viam.Instruction;

/**
 * Whereas {@link LlvmInstructionLoweringStrategy} defines multiple to lower {@link Instruction}
 * a.k.a Machine Instructions, this class lowers {@link CompilerInstruction}. But only as a fallback
 * strategy when no other strategy is applicable.
 */
public class LlvmCompilerInstructionLoweringDefaultStrategyImpl
    extends LlvmCompilerInstructionLowerStrategy {
  public LlvmCompilerInstructionLoweringDefaultStrategyImpl(
      List<LlvmInstructionLoweringStrategy> strategies) {
    super(strategies);
  }
}
