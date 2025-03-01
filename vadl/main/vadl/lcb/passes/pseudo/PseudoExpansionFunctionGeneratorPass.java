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

package vadl.lcb.passes.pseudo;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.Objects;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.pseudo.AbstractPseudoInstructionArgumentReplacementPass;
import vadl.gcb.passes.pseudo.PseudoInstructionArgumentReplacementPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * Expand "real" pseudo instructions which are defined in the specification.
 */
public class PseudoExpansionFunctionGeneratorPass
    extends AbstractPseudoExpansionFunctionGeneratorPass {
  public PseudoExpansionFunctionGeneratorPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PseudoExpansionFunctionGeneratorPass");
  }

  @Override
  protected Stream<Pair<PseudoInstruction, Graph>> getApplicable(
      PassResults passResults,
      Specification viam) {
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var appliedArguments =
        (AbstractPseudoInstructionArgumentReplacementPass.Output) passResults.lastResultOf(
            PseudoInstructionArgumentReplacementPass.class);

    var specifiedSequences = Stream.of(abi.returnSequence(), abi.callSequence());

    // We do not use the behavior of the pseudo instruction because each InstrCallNode
    // has the instruction behavior to the original instruction.
    // However, we did apply the arguments, and now we want to expand the pseudo instruction
    // with those arguments. That's why we have to use the result of a previous pass.
    var pseudoInstructions = Stream.concat(viam.isa()
            .map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty),
        specifiedSequences);
    return pseudoInstructions
        .map(pseudoInstruction -> {
          var appliedInstructions = appliedArguments.appliedGraph().get(pseudoInstruction);
          ensureNonNull(appliedInstructions,
              () -> Diagnostic.error("There is no graph with the applied arguments.",
                  pseudoInstruction.sourceLocation()));
          return Pair.of(pseudoInstruction, Objects.requireNonNull(appliedInstructions));
        });
  }
}
