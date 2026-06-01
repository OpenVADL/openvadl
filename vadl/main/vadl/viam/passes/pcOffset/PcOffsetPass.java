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
import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.annotations.PcOffsetAnnotation;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.passes.staticCounterAccess.CounterAccessResolvingPass;

/**
 * Applies program counter offsets to program counter reads.
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
    var isa = viam.isa().get();
    ViamUtils.findAllBehaviors(viam).forEach(behavior -> {
      var instruction = behavior.parentDefinition() instanceof Instruction instr ? instr : null;
      behavior.getNodes(ReadRegTensorNode.class)
          .forEach(n -> handleRead(n, instruction, isa));
    });
    return null;
  }

  private void handleRead(ReadRegTensorNode read, @Nullable Instruction instruction,
                          InstructionSetArchitecture isa) {
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
      instruction = ensureNonNull(instruction, () -> error(
          "Program counter read with offset can only happen in instruction behavior", read)
          .locationHelp(offsetAnn, "The program counter offset")
      );

      var memories = isa.ownMemories();
      ensure(memories.size() == 1, () -> error(
          "Exactly one memory definition required for reading program counter with offset", isa)
          .locationHelp(read, "The program counter read")
          .locationHelp(offsetAnn, "The program counter offset")
      );
      var memory = memories.getFirst();

      var instrBytes = instruction.format().type().bitWidth() / memory.resultType().bitWidth();
      read.replace(BuiltInCall.of(
          BuiltInTable.ADD, read,
          GraphUtils.intUNode((long) offset * instrBytes, read.type().bitWidth())
      ));
    }
  }
}
