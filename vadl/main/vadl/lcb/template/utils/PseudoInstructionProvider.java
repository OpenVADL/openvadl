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

package vadl.lcb.template.utils;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.stream.Stream;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Utility class for getting {@link PseudoInstruction}.
 */
public class PseudoInstructionProvider {
  /**
   * Get the list of {@link PseudoInstruction} which only contain {@link Instruction} which
   * are lowered to LLVM.
   */
  public static Stream<PseudoInstruction> getSupportedPseudoInstructions(
      Specification specification,
      PassResults passResults) {
    var supportedInstructions = ensureNonNull(
        (LlvmLoweringPass.LlvmLoweringPassResult) passResults.lastResultOf(LlvmLoweringPass.class),
        "llvmLoweringPass result must exist").machineInstructionRecords()
        .keySet();
    return specification.isa()
        .map(x -> x.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .filter(pseudoInstruction -> pseudoInstruction.behavior().getNodes(InstrCallNode.class)
            .allMatch(i -> {
              var isSupported = supportedInstructions.contains(i.target());
              if (!isSupported) {
                DeferredDiagnosticStore.add(Diagnostic.warning(
                    "Instruction was not lowered. "
                        + "Therefore, it cannot be used in the pseudo instruction",
                    i.sourceLocation()).build());
              }
              return isSupported;
            }));
  }
}
