// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.passes.pcOffset;

import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.intUNode;
import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Memory;
import vadl.viam.Specification;
import vadl.viam.annotations.PcOffsetAnnotation;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.InstructionWidthNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.passes.staticCounterAccess.CounterAccessResolvingPass;

/**
 * Applies program counter offsets to program counter reads and resolves
 * {@link InstructionWidthNode}s.
 *
 * <p>When reading a program counter that has been annotated with
 * {@code [current]}, {@code [next]} or {@code [next next]}, the
 * read value is offset by multiples of the instruction length. The subcalls
 * {@code .current}, {@code .next} or {@code .nextnext} overwrite the annotation,
 * but that logic is handled in {@code BehaviorLowering}.
 *
 * <p>This pass looks for {@link PcOffsetAnnotation}s and applies them to
 * {@link ReadRegTensorNode}s, which have access the program counter.
 *
 * <p>Inside {@link Instruction} behaviors, we can replace {@link InstructionWidthNode}s with the
 * constant value of the width of the instruction they are in. In other behaviors, such as
 * exceptions, this is not as trivial. The artifacts will have to implement special treatment in
 * these cases. Currently, {@link InstructionWidthNode}s outside instruction behaviors are replaced
 * with the width of the first found instruction in the ISA. This a temporary solution and only
 * works for ISAs with only one instruction length.
 *
 * <p><strong>Note</strong>: This pass must run after {@link CounterAccessResolvingPass}.
 */
public class PcOffsetPass extends Pass {

  public PcOffsetPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PC Offset Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    ViamUtils.findAllBehaviors(viam).forEach(
        behavior -> behavior.getNodes(ReadRegTensorNode.class).toList().forEach(this::handleRead)
    );
    new InstructionWidthNodeConverter(viam).run();
    return null;
  }

  private void handleRead(ReadRegTensorNode read) {
    if (read.staticCounterAccess() == null) {
      // this is not a pc access
      return;
    }
    var offsetAnn = read.resourceDefinition().annotation(PcOffsetAnnotation.class);
    if (offsetAnn == null) {
      return;
    }
    var offset = offsetAnn.offset();
    if (offset != 0) {
      var pcType = read.type();
      ExpressionNode offsetNode = BuiltInTable.MUL.call(
          intUNode(offset, pcType.bitWidth()), new InstructionWidthNode(pcType)
      );
      offsetNode.setSourceLocationRecursively(read.location());
      read.replace(BuiltInTable.ADD.call(read.shallowCopy(), offsetNode));
    }
  }
}

class InstructionWidthNodeConverter {

  Specification viam;
  InstructionSetArchitecture isa;

  @Nullable
  Memory memory;

  @Nullable
  Instruction anyInstruction;

  public InstructionWidthNodeConverter(Specification viam) {
    this.viam = viam;
    this.isa = viam.isa().get();
  }

  void run() {
    anyInstruction = isa.ownInstructions().stream().findAny().orElse(null);

    // FIXME: If the InstructionWidthNode is not in an instruction, we cannot resolve it here.
    //        For now, we use the width of the first instruction found in the ISA. If not
    //        instruction is present, we have to leave the InstructionWidthNode
    var firstInstr = isa.ownInstructions().stream().findFirst().orElse(null);

    ViamUtils.findAllBehaviors(viam).forEach(b -> {
      var instruction = b.parentDefinition() instanceof Instruction instr ? instr : firstInstr;
      if (instruction == null) {
        return;
      }
      var nodes = b.getNodes(InstructionWidthNode.class).toList();
      if (nodes.isEmpty()) {
        return;
      }
      var instrBytes = instruction.format().type().bitWidth()
          / memory(nodes.getFirst()).resultType().bitWidth();
      nodes.forEach(
          n -> n.replaceAndDelete(intUNode(instrBytes, n.type().asDataType().bitWidth())));
    });
  }

  private Memory memory(InstructionWidthNode node) {
    if (memory == null) {
      var memories = isa.ownMemories();
      ensure(memories.size() == 1, () ->
          error("Exactly one memory definition required to infer how many bits are in a byte", isa)
              .locationDescription(node, "Byte size required here")
      );
      memory = memories.getFirst();
    }
    return memory;
  }

}
