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
import java.nio.ByteOrder;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.IrregularDecodeTreeGenerator;
import vadl.vdt.impl.irregular.model.DecodeEntry;
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

    // TODO: get the byte order from the VADL specification -> Implement memory annotations
    final ByteOrder bo = ByteOrder.LITTLE_ENDIAN;

    var insns = isa.ownInstructions()
        .stream()
        .map(i -> {
          BitPattern pattern = PatternUtils.toFixedBitPattern(i, bo);
          return new DecodeEntry(i, pattern.width(), pattern, Set.of());
        })
        .toList();

    if (insns.isEmpty()) {
      // just skip if there are no instructions.
      // this will only happen if we use the check command
      return null;
    }

    if (isa.simpleName().equals("A64")) {
      // TODO: Switch to the irregular tree generator, once we support encoding constraints
      return new RegularDecodeTreeGenerator()
          .generate(insns.stream()
              .map(Instruction.class::cast).toList());
    }

    return new IrregularDecodeTreeGenerator().generate(insns);
  }
}
