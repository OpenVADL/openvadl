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

package vadl.gcb.passes.pseudo;

import java.util.Collections;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * Applies the arguments of an {@link Instruction} of a {@link PseudoInstruction}.
 */
public class PseudoInstructionArgumentReplacementPass
    extends AbstractPseudoInstructionArgumentReplacementPass {
  public PseudoInstructionArgumentReplacementPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected Stream<PseudoInstruction> getApplicable(PassResults passResults, Specification viam) {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();
    // Pseudo Instructions + ABI sequences
    return
        Stream.concat(
            viam.isa()
                .map(InstructionSetArchitecture::ownPseudoInstructions)
                .orElse(Collections.emptyList())
                .stream(),
            Stream.of(abi.returnSequence(), abi.callSequence())
        );
  }

  @Override
  public PassName getName() {
    return new PassName("PseudoInstructionArgumentReplacementPass");
  }
}
