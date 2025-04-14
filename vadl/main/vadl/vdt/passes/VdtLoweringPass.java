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
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.IrregularDecodeTreeGenerator;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.model.Node;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.PatternUtils;
import vadl.viam.Processor;
import vadl.viam.Specification;
import vadl.viam.ViamError;

/**
 * Lowering pass that creates the VDT (VADL Decode Tree) from the VIAM definition.
 */
public class VdtLoweringPass extends AbstractIssPass {

  /**
   * Constructor for the VDT Lowering Pass.
   *
   * @param configuration the configuration
   */
  public VdtLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Lowering");
  }

  @Override
  public @Nullable Node execute(PassResults passResults, Specification viam)
      throws IOException {

    // TODO: handle inheritance correctly
    var isa = viam.processor().map(Processor::isa).orElse(null);
    if (isa == null) {
      throw new ViamError("No ISA found in the specification");
    }

    // TODO: get the byte order from the VADL specification
    final ByteOrder bo = ByteOrder.LITTLE_ENDIAN;

    var insns = isa.ownInstructions()
        .stream()
        .map(i -> {
          BitPattern pattern = PatternUtils.toFixedBitPattern(i, bo);
          // TODO: construct exclusion patterns from encoding constraints
          return new DecodeEntry(i, pattern.width(), pattern, Set.of());
        })
        .toList();

    return new IrregularDecodeTreeGenerator().generate(insns);
  }
}
