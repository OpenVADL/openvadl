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

package vadl.gcb.passes.operands;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.operands.model.GcbDefaultInstructionOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.CompilerInstruction;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Generates the input and output operands for instructions.
 */
public class GenerateInstructionOperandsPass extends Pass {

  /**
   * Constructor.
   */
  public GenerateInstructionOperandsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  public record Output(MachineInstructionAggregate machineInstructions,
                       CompilerInstructionAggregate compilerInstructions) {

  }

  public record MachineInstructionAggregate(Map<Instruction, List<GcbInstructionOperand>> inputs,
                                            Map<Instruction, List<GcbInstructionOperand>> outputs) {

  }

  public record CompilerInstructionAggregate(
      Map<CompilerInstruction, List<GcbInstructionOperand>> inputs,
      Map<CompilerInstruction, List<GcbInstructionOperand>> outputs) {

  }


  @Override
  public PassName getName() {
    return new PassName("GenerateInstructionOperandsPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var machineInstructions = machineInstructions(viam);

    return null;
  }

  private MachineInstructionAggregate machineInstructions(Specification viam) {
    var inputs = new IdentityHashMap<Instruction, List<GcbInstructionOperand>>();
    var outputs = new IdentityHashMap<Instruction, List<GcbInstructionOperand>>();

    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var outputOperands = getOutputOperands(instruction.behavior());
    }

    return new MachineInstructionAggregate(inputs, outputs);
  }

  private List<GcbInstructionOperand> getOutputOperands(Graph behavior) {
    var operands = extractWrites(behavior);
    return operands
        .stream()
        .filter(operand -> {
          // Why?
          // Because LLVM cannot handle static registers in input or output operands.
          // They belong to defs and uses instead.
          return !operand.hasConstantAddress();
        })
        .map(this::map)
        .toList();
  }

  private GcbInstructionOperand map(WriteRegTensorNode x) {
  }

  /**
   * Most instruction's behaviors have outputs. Those are the results which the instruction emits.
   */
  private List<WriteRegTensorNode> extractWrites(Graph graph) {
    return graph.getNodes(WriteRegTensorNode.class)
        .filter(e -> e.regTensor().isRegisterFile())
        .toList();
  }
}
