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

import static vadl.configuration.DecoderOptions.Generator.OCC;
import static vadl.configuration.DecoderOptions.Generator.REGULAR;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.IrregularDecodeTreeGenerator;
import vadl.vdt.impl.irregular.OccurrenceAwareDecodeTreeGenerator;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.utils.Instruction;
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
  @SuppressWarnings("unchecked")
  public @Nullable Node execute(PassResults passResults, Specification viam)
      throws IOException {

    final List<DecodeEntry> entries;
    if (passResults.hasRunPassOnce(VdtConstraintSynthesisPass.class)) {
      entries =
          (List<DecodeEntry>) passResults.lastNullableResultOf(VdtConstraintSynthesisPass.class);
    } else {
      entries = (List<DecodeEntry>) passResults.lastNullableResultOf(VdtInputPreparationPass.class);
    }

    if (entries == null) {
      // just skip if there are no instructions.
      // this will only happen if we use the check command
      return null;
    }

    final var generator = configuration().getDecoderOptions().getGenerator();

    if (generator == REGULAR) {
      var insns = entries.stream()
          .map(Instruction.class::cast)
          .toList();
      return new RegularDecodeTreeGenerator().generate(insns);
    }

    if (generator == OCC) {
      // TODO: Add the option to specify the memory penalty
      return new OccurrenceAwareDecodeTreeGenerator(1).generate(entries);
    }

    return new IrregularDecodeTreeGenerator().generate(entries);
  }
}
