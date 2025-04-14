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

package vadl.vdt.passes;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PatternUtils;
import vadl.viam.Specification;

/**
 * Lowering pass that creates the VDT (VADL Decode Tree) from the VIAM definition.
 */
public class VdtLoweringPass extends Pass {

  /**
   * Constructor for the VDT Lowering Pass.
   *
   * @param configuration the configuration
   */
  public VdtLoweringPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Lowering");
  }

  @Override
  public @Nullable Node execute(PassResults passResults, Specification viam)
      throws IOException {

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return null;
    }

    var insns = isa.ownInstructions()
        .stream()
        .map(this::prepareInstruction)
        .toList();

    if (insns.isEmpty()) {
      // just skip if there are no instructions.
      // this will only happen if we use the check command
      return null;
    }

    return new RegularDecodeTreeGenerator().generate(insns);
  }

  /**
   * Prepares an instruction for the decode tree generation.
   *
   * @param insn The VIAM instruction
   * @return The prepared instruction
   */
  private Instruction prepareInstruction(vadl.viam.Instruction insn) {
    BitPattern pattern = PatternUtils.toFixedBitPattern(insn);
    return new Instruction(insn, pattern.width(), pattern);
  }
}
