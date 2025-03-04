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

package vadl.lcb.passes.isaMatching;

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * A {@link InstructionSetArchitecture} contains a {@link List} of {@link PseudoInstruction}.
 * One of the most important tasks of the LCB is to recognize the semantics of the machine
 * instruction and create a mapping from LLVM's SelectionDag to the machine instruction.
 * Most instructions in an instruction set architecture will be "simple" and
 * a lot of them are required in almost every instruction set architecture. The goal
 * of {@link IsaPseudoInstructionMatchingPass} to label instructions which can be recognized.
 * It works like {@link IsaMachineInstructionMatchingPass} but for {@link PseudoInstruction}.
 */
public class IsaPseudoInstructionMatchingPass extends Pass implements IsaMatchingUtils {
  public IsaPseudoInstructionMatchingPass(LcbConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("IsaPseudoInstructionMatchingPass");
  }

  /**
  * Result of the pass.
*/
  public record Result(Map<PseudoInstructionLabel, List<PseudoInstruction>> labels,
                       Map<PseudoInstruction, PseudoInstructionLabel> reverse) {

  }


  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    // The instruction matching happens on the uninlined graph
    // because the field accesses are uninlined.
    IdentityHashMap<Instruction, UninlinedGraph> uninlined =
        ((FunctionInlinerPass.Output) passResults
            .lastResultOf(FunctionInlinerPass.class)).behaviors();
    Objects.requireNonNull(uninlined);
    var supportedMachineInstructions =
        (IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class);

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return new Result(Collections.emptyMap(), Collections.emptyMap());
    }

    isa.ownPseudoInstructions().forEach(pseudoInstruction -> {
      if (findUnconditionalJump(supportedMachineInstructions.reverse(), pseudoInstruction)) {
        pseudoInstruction.attachExtension(new PseudoInstructionCtx(PseudoInstructionLabel.J));
      } else if (findLi(supportedMachineInstructions.reverse(), pseudoInstruction)) {
        pseudoInstruction.attachExtension(new PseudoInstructionCtx(PseudoInstructionLabel.LI));
      }
    });

    var labels = createPseudoLabelMap(viam);
    return new Result(labels, flipIsaMatching(labels));
  }

  private boolean findLi(Map<Instruction, MachineInstructionLabel> flipped,
                         PseudoInstruction pseudoInstruction) {
    if (pseudoInstruction.behavior().getNodes(InstrCallNode.class).count() != 2) {
      return false;
    }

    var instrCallNodes =
        pseudoInstruction.behavior().getNodes(InstrCallNode.class).toList();
    var firstNode = instrCallNodes.get(0);
    var secondNode = instrCallNodes.get(1);

    return firstNode != null && secondNode != null
        && flipped.get(firstNode.target()) == MachineInstructionLabel.LUI
        && (flipped.get(secondNode.target()) == MachineInstructionLabel.ADDI_32
        || flipped.get(secondNode.target()) == MachineInstructionLabel.ADDI_64);
  }

  private boolean findUnconditionalJump(
      Map<Instruction, MachineInstructionLabel> flipped,
      PseudoInstruction pseudoInstruction) {
    if (pseudoInstruction.behavior().getNodes(InstrCallNode.class).count() != 1) {
      return false;
    }

    var instrCallNode =
        ensurePresent(pseudoInstruction.behavior().getNodes(InstrCallNode.class).findFirst(),
            "instruction call node must exist");
    var machineInstructionLabel = flipped.get(instrCallNode.target());
    return machineInstructionLabel == MachineInstructionLabel.JAL;
  }
}
