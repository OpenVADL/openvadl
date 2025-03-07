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

package vadl.lcb.riscv.riscv64;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.llvmLowering.GenerateTableGenMachineInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;

public class CheckMachineInstructionTableGenOperandsRiscv64PassTest extends AbstractLcbTest {

  private static final Set<Entry> inputOperands = new HashSet<>();
  private static final Set<Entry> outputOperands = new HashSet<>();

  static class Entry {
    private final String instruction;
    private final List<String> operands;

    public Entry(String instruction, List<String> operands) {
      this.instruction = instruction;
      this.operands = operands;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Entry entry && instruction.equals(entry.instruction)
          && entry.operands.equals(((Entry) obj).operands);
    }
  }

  static {
    inputOperands.add(new Entry("ADD", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("ADDI", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("AND", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("ANDI", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(
        new Entry("BEQ", List.of("X:$rs1", "X:$rs2", "RV3264I_Btype_immAsLabel:$imm")));
    inputOperands.add(
        new Entry("BGE", List.of("X:$rs1", "X:$rs2", "RV3264I_Btype_immAsLabel:$imm")));
    inputOperands.add(
        new Entry("BGEU", List.of("X:$rs1", "X:$rs2", "RV3264I_Btype_immAsLabel:$imm")));
    inputOperands.add(
        new Entry("BLT", List.of("X:$rs1", "X:$rs2", "RV3264I_Btype_immAsLabel:$imm")));
    inputOperands.add(
        new Entry("BLTU", List.of("X:$rs1", "X:$rs2", "RV3264I_Btype_immAsLabel:$imm")));
    inputOperands.add(
        new Entry("BNE", List.of("X:$rs1", "X:$rs2", "RV3264I_Btype_immAsLabel:$imm")));
    inputOperands.add(new Entry("DIV", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("DIVU", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("JAL", List.of("RV3264I_Jtype_immAsInt64:$imm")));
    inputOperands.add(new Entry("JALR", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("LB", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("LBU", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("LH", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("LHU", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("LW", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("LWU", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("LUI", List.of("RV3264I_Utype_immAsInt64:$imm")));
    inputOperands.add(new Entry("MUL", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("MULH", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("MULHSU", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("MULHU", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("OR", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("ORI", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("REM", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("REMU", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(
        new Entry("SW", List.of("X:$rs1", "X:$rs2", "RV3264I_Stype_immAsInt64:$imm")));
    inputOperands.add(
        new Entry("SB", List.of("X:$rs1", "X:$rs2", "RV3264I_Stype_immAsInt64:$imm")));
    inputOperands.add(
        new Entry("SD", List.of("X:$rs1", "X:$rs2", "RV3264I_Stype_immAsInt64:$imm")));
    inputOperands.add(
        new Entry("SH", List.of("X:$rs1", "X:$rs2", "RV3264I_Stype_immAsInt64:$imm")));
    inputOperands.add(new Entry("SLL", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("SLLI", List.of("X:$rs1", "RV3264I_Ftype_sftAsInt64:$sft")));
    inputOperands.add(new Entry("SLT", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("SLTI", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("SLTIU", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));
    inputOperands.add(new Entry("SLTU", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("SRA", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("SRAI", List.of("X:$rs1", "RV3264I_Ftype_sftAsInt64:$sft")));
    inputOperands.add(new Entry("SRL", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("SRLI", List.of("X:$rs1", "RV3264I_Ftype_sftAsInt64:$sft")));
    inputOperands.add(new Entry("SUB", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("XOR", List.of("X:$rs1", "X:$rs2")));
    inputOperands.add(new Entry("XORI", List.of("X:$rs1", "RV3264I_Itype_immAsInt64:$imm")));

    outputOperands.add(new Entry("ADD", List.of("X:$rd")));
    outputOperands.add(new Entry("ADDI", List.of("X:$rd")));
    outputOperands.add(new Entry("AND", List.of("X:$rd")));
    outputOperands.add(new Entry("ANDI", List.of("X:$rd")));
    outputOperands.add(
        new Entry("BEQ", List.of()));
    outputOperands.add(
        new Entry("BGE", List.of()));
    outputOperands.add(
        new Entry("BGEU", List.of()));
    outputOperands.add(
        new Entry("BLT", List.of()));
    outputOperands.add(
        new Entry("BLTU", List.of()));
    outputOperands.add(
        new Entry("BNE", List.of()));
    outputOperands.add(new Entry("DIV", List.of("X:$rd")));
    outputOperands.add(new Entry("DIVU", List.of("X:$rd")));
    outputOperands.add(new Entry("JAL", List.of("X:$rd")));
    outputOperands.add(new Entry("JALR", List.of("X:$rd")));
    outputOperands.add(new Entry("LB", List.of("X:$rd")));
    outputOperands.add(new Entry("LBU", List.of("X:$rd")));
    outputOperands.add(new Entry("LH", List.of("X:$rd")));
    outputOperands.add(new Entry("LHU", List.of("X:$rd")));
    outputOperands.add(new Entry("LW", List.of("X:$rd")));
    outputOperands.add(new Entry("LWU", List.of("X:$rd")));
    outputOperands.add(new Entry("LUI", List.of("X:$rd")));
    outputOperands.add(new Entry("MUL", List.of("X:$rd")));
    outputOperands.add(new Entry("MULH", List.of("X:$rd")));
    outputOperands.add(new Entry("MULHSU", List.of("X:$rd")));
    outputOperands.add(new Entry("MULHU", List.of("X:$rd")));
    outputOperands.add(new Entry("OR", List.of("X:$rd")));
    outputOperands.add(new Entry("ORI", List.of("X:$rd")));
    outputOperands.add(new Entry("REM", List.of("X:$rd")));
    outputOperands.add(new Entry("REMU", List.of("X:$rd")));
    outputOperands.add(
        new Entry("SW", List.of()));
    outputOperands.add(
        new Entry("SB", List.of()));
    outputOperands.add(
        new Entry("SD", List.of()));
    outputOperands.add(
        new Entry("SH", List.of()));
    outputOperands.add(new Entry("SLL", List.of("X:$rd")));
    outputOperands.add(new Entry("SLLI", List.of("X:$rd")));
    outputOperands.add(new Entry("SLT", List.of("X:$rd")));
    outputOperands.add(new Entry("SLTI", List.of("X:$rd")));
    outputOperands.add(new Entry("SLTIU", List.of("X:$rd")));
    outputOperands.add(new Entry("SLTU", List.of("X:$rd")));
    outputOperands.add(new Entry("SRA", List.of("X:$rd")));
    outputOperands.add(new Entry("SRAI", List.of("X:$rd")));
    outputOperands.add(new Entry("SRL", List.of("X:$rd")));
    outputOperands.add(new Entry("SRLI", List.of("X:$rd")));
    outputOperands.add(new Entry("SUB", List.of("X:$rd")));
    outputOperands.add(new Entry("XOR", List.of("X:$rd")));
    outputOperands.add(new Entry("XORI", List.of("X:$rd")));
  }

  @TestFactory
  List<DynamicTest> shouldHaveInputOperands()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateTableGenMachineInstructionRecordPass.class.getName()));
    var passManager = setup.passManager();

    // When
    var machineInstructionRecords = (List<TableGenMachineInstruction>) passManager.getPassResults()
        .lastResultOf(GenerateTableGenMachineInstructionRecordPass.class);

    // Then
    var actual = machineInstructionRecords.stream()
        .map(x -> new Entry(x.instruction().identifier.simpleName(),
            x.getInOperands().stream().map(TableGenInstructionOperand::render).toList())
        ).collect(Collectors.toMap(x -> x.instruction, x -> x.operands));

    return inputOperands.stream()
        .map(entry -> {
          var value = actual.get(entry.instruction);

          return DynamicTest.dynamicTest(entry.instruction, () -> {
            Assertions.assertThat(value).isNotNull();
            Assertions.assertThat(entry.operands).isEqualTo(value);
          });
        })
        .toList();
  }

  @TestFactory
  List<DynamicTest> shouldHaveOutputOperands()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateTableGenMachineInstructionRecordPass.class.getName()));
    var passManager = setup.passManager();

    // When
    var machineInstructionRecords = (List<TableGenMachineInstruction>) passManager.getPassResults()
        .lastResultOf(GenerateTableGenMachineInstructionRecordPass.class);

    // Then
    var actual = machineInstructionRecords.stream()
        .map(x -> new Entry(x.instruction().identifier.simpleName(),
            x.getOutOperands().stream().map(TableGenInstructionOperand::render).toList())
        ).collect(Collectors.toMap(x -> x.instruction, x -> x.operands));

    return outputOperands.stream()
        .map(entry -> {
          var value = actual.get(entry.instruction);

          return DynamicTest.dynamicTest(entry.instruction, () -> {
            Assertions.assertThat(value).isNotNull();
            Assertions.assertThat(entry.operands).isEqualTo(value);
          });
        })
        .toList();
  }

}
