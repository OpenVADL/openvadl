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

package vadl.gcb.riscv.riscv64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import vadl.gcb.AbstractGcbTest;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public class IdentifyFieldUsagePassTest extends AbstractGcbTest {


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
        jtypeImm("JALR"),
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
    return Arguments.of(instruction, List.of("imm"));
  }

  private static Arguments utypeImm(String instruction) {
    return Arguments.of(instruction, List.of("imm"));
  }

  private static Arguments ltypeImm(String instruction) {
    return Arguments.of(instruction, List.of("imm"));
  }

  private static Arguments stypeImm(String instruction) {
    return Arguments.of(instruction, List.of("imm"));
  }

  private static Arguments btypeImm(String instruction) {
    return Arguments.of(instruction, List.of("imm"));
  }

  private static Arguments jtypeImm(String instruction) {
    return Arguments.of(instruction, List.of("imm"));
  }

  private static Arguments wshiftInstr(String instruction) {
    return Arguments.of(instruction, List.of("rs2"));
  }

  private static Arguments ftypeImm(String instruction) {
    return Arguments.of(instruction, List.of("sft"));
  }

  @MethodSource(value = "immediates")
  @ParameterizedTest
  void shouldDetectImmediates(String instructionName, List<String> imms)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(IdentifyFieldUsagePass.class.getName()));
    var passManager = setup.passManager();

    // When
    var result =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passManager.getPassResults()
            .lastResultOf(IdentifyFieldUsagePass.class);

    // Then
    Assertions.assertNotNull(result);
    var instruction = getInstrByName(instructionName, setup.specification());
    Assertions.assertNotNull(instruction);
    var immediates = result.getImmediates(instruction);

    for (var imm : imms) {
      assertThat(getImmediate(imm, immediates)).isPresent();
    }
  }

  private static Stream<Arguments> registers() {
    return Stream.of(
        rtypeRegisters("ADD"),
        rtypeRegisters("SUB"),
        rtypeRegisters("AND"),
        rtypeRegisters("OR"),
        rtypeRegisters("XOR"),
        rtypeRegisters("SLT"),
        rtypeRegisters("SLTU"),
        rtypeRegisters("SLL"),
        rtypeRegisters("SRL"),
        rtypeRegisters("SRA"),
        itypeRegisters("ADDI"),
        itypeRegisters("ANDI"),
        itypeRegisters("ORI"),
        itypeRegisters("XORI"),
        itypeRegisters("SLTI"),
        itypeRegisters("SLTIU"),
        utypeRegisters("AUIPC"),
        utypeRegisters("LUI"),
        ltypeRegisters("LB"),
        ltypeRegisters("LBU"),
        ltypeRegisters("LH"),
        ltypeRegisters("LHU"),
        ltypeRegisters("LW"),
        stypeRegisters("SB"),
        stypeRegisters("SH"),
        stypeRegisters("SW"),
        btypeRegisters("BEQ"),
        btypeRegisters("BNE"),
        btypeRegisters("BGE"),
        btypeRegisters("BGEU"),
        btypeRegisters("BLT"),
        btypeRegisters("BLTU"),
        jtypeRegisters("JAL"),
        itypeRegisters("JALR"),
        ltypeRegisters("LWU"),
        ltypeRegisters("LD"),
        stypeRegisters("SD"),
        Arguments.of("ADDIW", List.of("rd", "rs1")),
        Arguments.of("SLLIW", List.of("rd", "rs1")),
        Arguments.of("SRLIW", List.of("rd", "rs1")),
        Arguments.of("SRAIW", List.of("rd", "rs1")),
        wtypeRegisters("ADDW"),
        wtypeRegisters("SUBW"),
        wtypeRegisters("SLLW"),
        wtypeRegisters("SRLW"),
        wtypeRegisters("SRAW"),
        Arguments.of("SLLI", List.of("rd", "rs1")),
        Arguments.of("SRLI", List.of("rd", "rs1")),
        Arguments.of("SRAI", List.of("rd", "rs1"))
    );
  }

  private static Arguments rtypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rs1", "rs2", "rd"));
  }

  private static Arguments itypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rs1", "rd"));
  }

  private static Arguments utypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rd"));
  }

  private static Arguments stypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rs1", "rs2"));
  }

  private static Arguments btypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rs1", "rs2"));
  }

  private static Arguments jtypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rd"));
  }

  private static Arguments ltypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rd", "rs1"));
  }

  private static Arguments wtypeRegisters(String instruction) {
    return Arguments.of(instruction, List.of("rd", "rs1", "rs2"));
  }


  @MethodSource(value = "registers")
  @ParameterizedTest
  void shouldDetectRegister(String instructionName, List<String> regs)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(IdentifyFieldUsagePass.class.getName()));
    var passManager = setup.passManager();

    // When
    var result =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passManager.getPassResults()
            .lastResultOf(IdentifyFieldUsagePass.class);

    // Then
    Assertions.assertNotNull(result);
    var instruction = getInstrByName(instructionName, setup.specification());
    Assertions.assertNotNull(instruction);
    var registers = result.getRegisterUsages(instruction);

    for (var reg : regs) {

      assertThat(getRegister(reg, registers)).isPresent();
    }
  }

  private Optional<Format.Field> getRegister(String reg,
                                             Map<Format.Field, IdentifyFieldUsagePass.RegisterUsageAggregate> registers) {
    return registers.keySet().stream()
        .filter(
            registerUsageAggregate -> registerUsageAggregate.identifier.simpleName().equals(reg))
        .findFirst();
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
