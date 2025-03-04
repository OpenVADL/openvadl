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

import java.io.IOException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.passes.llvmLowering.strategies.nodeLowering.LcbConstantNodeReplacement;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * LLVM only implements fixed width immediates. Therefore, we need to
 * uplift any constant with an arbitrary bit width.
 * Note it does not mutate the graph on the viam, but only the result of
 * {@link PseudoInstructionArgumentReplacementPass}.
 */
public class PseudoConstantUpliftingPass extends Pass {
  public PseudoConstantUpliftingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PseudoConstantUpliftingPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    /*
    var pseudoInstructions = viam.isa()
        .map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .toList();
    var appliedArguments =
        (PseudoInstructionArgumentReplacementPass.Output) passResults.lastResultOf(
            PseudoInstructionArgumentReplacementPass.class);

    for (var pseudoInstruction : pseudoInstructions) {
      var behaviorWithAppliedArguments =
          appliedArguments.appliedGraph().get(pseudoInstruction);
      if (behaviorWithAppliedArguments != null) {
        behaviorWithAppliedArguments
            .getNodes(InstrCallNode.class)
            .map(instrCallNode -> instrCallNode.target().behavior())
            .flatMap(graph -> graph.getNodes(ConstantNode.class))
            .forEach(LcbConstantNodeReplacement::updateConstant);
      }
    }
     */

    return null;
  }
}
