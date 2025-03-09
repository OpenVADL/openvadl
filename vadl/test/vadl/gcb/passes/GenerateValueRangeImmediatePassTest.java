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

package vadl.gcb.passes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import vadl.gcb.AbstractGcbTest;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public class GenerateValueRangeImmediatePassTest extends AbstractGcbTest {


  public static Stream<Arguments> immediates() {
    return Stream.of(
        rtypeImm("ADD"),
        rtypeImm("SUB"),
        rtypeImm("OR"),
        rtypeImm("XOR"),
        rtypeImm("SLT"),
        rtypeImm("SLTU"),
        rtypeImm("SLL"),
        rtypeImm("SRL"),
        rtypeImm("SRA"),
        itypeImm("ADDI"),
        itypeImm("ANDI"),
        itypeImm("ORI"),
        itypeImm("XORI"),
        itypeImm("SLTI"),
        itypeImm("SLTIU"),
        itypeImm("SLTIU"),
        utypeImm("AUIPC"),
        utypeImm("LUI"),
        ltypeImm("LB"),
        ltypeImm("LBU"),
        ltypeImm("LH"),
        ltypeImm("LHU"),
        ltypeImm("LW"),
        stypeImm("SB"),
        stypeImm("SH"),
        stypeImm("SW"),
        btypeImm("BEQ"),
        btypeImm("BNE"),
        btypeImm("BGE"),
        btypeImm("BGEU"),
        btypeImm("BLT"),
        btypeImm("BLTU"),
        jtypeImm("JAL"),
        itypeImm("JALR"),
        ltypeImm("LWU"),
        ltypeImm("LD"),
        itypeImm("ADDIW"),
        wshiftInstr("SLLIW"),
        wshiftInstr("SRLIW"),
        wshiftInstr("SRAIW"),
        rtypeImm("ADDW"),
        rtypeImm("SUBW"),
        rtypeImm("SLLW"),
        rtypeImm("SRLW"),
        rtypeImm("SRAW"),
        ftypeImm("SLLI"),
        ftypeImm("SRLI"),
        ftypeImm("SRAI")
    );
  }

  private static Arguments rtypeImm(String instruction) {
    return Arguments.of(instruction, Collections.emptyList());
  }

  private static Arguments itypeImm(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("imm", new ValueRange(-2048, 2047))));
  }

  private static Arguments utypeImm(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("imm", new ValueRange(-524288, 524287))));
  }

  private static Arguments ltypeImm(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("imm", new ValueRange(-2048, 2047))));
  }

  private static Arguments stypeImm(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("imm", new ValueRange(-2048, 2047))));
  }

  private static Arguments btypeImm(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("imm", new ValueRange(-2048, 2047))));
  }

  private static Arguments jtypeImm(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("imm", new ValueRange(-524288, 524287))));
  }

  private static Arguments wshiftInstr(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("rs2", new ValueRange(0, 31))));
  }

  private static Arguments ftypeImm(String instruction) {
    return Arguments.of(instruction, List.of(Pair.of("sft", new ValueRange(0, 63))));
  }

  @MethodSource(value = "immediates")
  @ParameterizedTest
  void shouldDetectImmediates(String instructionName, List<Pair<String, ValueRange>> imms)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateValueRangeImmediatePass.class.getName()));

    // When

    // no result expected

    // Then
    var instruction = getInstrByName(instructionName, setup.specification());
    Assertions.assertNotNull(instruction);
    var valueRangeCtx = instruction.expectExtension(ValueRangeCtx.class);
    Assertions.assertNotNull(valueRangeCtx);

    for (var pair : imms) {
      var field = getField(instruction.format(), pair.left());
      var actualRange = valueRangeCtx.ranges().get(field);
      Assertions.assertNotNull(actualRange);
      Assertions.assertEquals(pair.right(), actualRange);
    }
  }

  private Format.Field getField(Format format, String field) {
    return Arrays.stream(format.fields())
        .filter(x -> x.identifier.simpleName().equals(field))
        .findFirst().get();
  }

  private static @Nonnull Optional<Format.Field> getImmediate(String imm,
                                                              List<Format.Field> immediates) {
    return immediates.stream().filter(x -> x.identifier.simpleName().equals(imm)).findFirst();
  }

  @Nullable
  private Instruction getInstrByName(String instruction,
                                     Specification specification) {
    return specification.isa().map(x -> x.ownInstructions().stream()).orElse(Stream.empty())
        .filter(x -> x.simpleName().equals(instruction))
        .findFirst()
        .get();
  }
}
