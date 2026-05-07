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

package vadl.asm;

import java.nio.ByteOrder;
import java.util.Arrays;
import vadl.viam.InstructionSetArchitecture;

/**
 * High-level facade over {@link InstructionEncoder} and {@link AssemblerSession}.
 *
 * <p>The facade owns an ISA-bound instruction encoder plus a symbolic assembler session and
 * exposes section-local operations for emitting instructions, bytes, labels, and label-relative
 * fixups without manually coordinating both lower-level APIs.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * var program = new ProgramAssembler(isa, ByteOrder.LITTLE_ENDIAN);
 * var text = program.text();
 * var data = program.data();
 *
 * text.label("start");
 * text.emit("ADDI",
 *     InstructionEncoder.Operand.of("rd", 1),
 *     InstructionEncoder.Operand.of("rs1", 1),
 *     InstructionEncoder.Operand.of("imm", 1));
 * text.emitLabelRelative("BEQ", "done", "immS",
 *     InstructionEncoder.Operand.of("rs1", 1),
 *     InstructionEncoder.Operand.of("rs2", 2));
 *
 * data.align(16);
 * data.label("result");
 * data.writeBytes(new byte[128]);
 *
 * text.label("done");
 * byte[] textBytes = program.session().resolvedSectionBytes(".text");
 * }</pre>
 */
public final class ProgramAssembler {

  public final class ProgramSection {

    private final AssemblerSession.Section section;

    private ProgramSection(AssemblerSession.Section section) {
      this.section = section;
    }

    public String name() {
      return section.name();
    }

    public int size() {
      return section.size();
    }

    public void label(String labelName) {
      section.label(labelName);
    }

    public void align(int alignment) {
      section.align(alignment);
    }

    public void writeBytes(byte[] bytes) {
      section.writeBytes(bytes);
    }

    public void emit32LittleEndian(int word) {
      section.emit32LittleEndian(word);
    }

    public void emit(String instructionName, InstructionEncoder.Operand... operands) {
      emit32LittleEndian(encoder.encode32(instructionName, operands));
    }

    public void emitLabelRelative(String instructionName, String targetLabel,
                                  String relativeOperandName,
                                  InstructionEncoder.Operand... operands) {
      emitLabelRelative(instructionName, name(), targetLabel, relativeOperandName, operands);
    }

    public void emitLabelRelative(String instructionName, String targetSectionName,
                                  String targetLabel, String relativeOperandName,
                                  InstructionEncoder.Operand... operands) {
      int patchOffset = size();
      session.addLabelFixup(name(), patchOffset, targetSectionName, targetLabel,
          (context, sectionBytes) -> sectionBytes.putInt(context.sourceOffset(),
              encoder.encode32(instructionName,
                  appendOperand(operands,
                      InstructionEncoder.Operand.of(relativeOperandName, context.relativeOffset()))
              )));
      emit32LittleEndian(0);
    }
  }

  private final InstructionEncoder encoder;
  private final AssemblerSession session = new AssemblerSession();

  public ProgramAssembler(InstructionSetArchitecture isa, ByteOrder byteOrder) {
    this.encoder = new InstructionEncoder(isa, byteOrder);
  }

  public static ProgramAssembler forIsa(InstructionSetArchitecture isa, ByteOrder byteOrder) {
    return new ProgramAssembler(isa, byteOrder);
  }

  public InstructionEncoder encoder() {
    return encoder;
  }

  public AssemblerSession session() {
    return session;
  }

  public ProgramSection section(String name) {
    return new ProgramSection(session.section(name));
  }

  public ProgramSection text() {
    return section(".text");
  }

  public ProgramSection data() {
    return section(".data");
  }

  private static InstructionEncoder.Operand[] appendOperand(InstructionEncoder.Operand[] operands,
                                                            InstructionEncoder.Operand appended) {
    var result = Arrays.copyOf(operands, operands.length + 1);
    result[operands.length] = appended;
    return result;
  }
}
