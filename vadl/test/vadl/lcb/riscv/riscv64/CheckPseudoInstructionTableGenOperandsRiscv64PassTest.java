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
import vadl.lcb.passes.llvmLowering.GenerateTableGenPseudoInstructionRecordPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;

public class CheckPseudoInstructionTableGenOperandsRiscv64PassTest extends AbstractLcbTest {

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
    inputOperands.add(new Entry("BEQZ", List.of("X:$rs", "bare_symbol:$offset")));
    inputOperands.add(new Entry("BGEZ", List.of("X:$rs", "bare_symbol:$offset")));
    inputOperands.add(new Entry("BGTZ", List.of("X:$rs", "bare_symbol:$offset")));
    inputOperands.add(new Entry("BLEZ", List.of("X:$rs", "bare_symbol:$offset")));
    inputOperands.add(new Entry("BLTZ", List.of("X:$rs", "bare_symbol:$offset")));
    inputOperands.add(new Entry("BNEZ", List.of("X:$rs", "bare_symbol:$offset")));
    inputOperands.add(new Entry("CALL", List.of("bare_symbol:$symbol")));
    inputOperands.add(new Entry("J", List.of("RV3264Base_J_immSAsLabel:$imm")));
    inputOperands.add(new Entry("LI", List.of("bare_symbol:$symbol")));
    inputOperands.add(new Entry("LLA", List.of("bare_symbol:$symbol")));
    inputOperands.add(new Entry("MV", List.of("X:$rs1")));
    inputOperands.add(new Entry("NEG", List.of("X:$rs1")));
    inputOperands.add(new Entry("NOP", List.of()));
    inputOperands.add(new Entry("NOT", List.of("X:$rs1")));
    inputOperands.add(new Entry("RET", List.of()));
    inputOperands.add(new Entry("SGTZ", List.of("X:$rs1")));
    inputOperands.add(new Entry("SLTZ", List.of("X:$rs1")));
    inputOperands.add(new Entry("SNEZ", List.of("X:$rs1")));
    inputOperands.add(new Entry("TAIL", List.of("bare_symbol:$symbol")));

    outputOperands.add(new Entry("BEQZ", List.of()));
    outputOperands.add(new Entry("BGEZ", List.of()));
    outputOperands.add(new Entry("BGTZ", List.of()));
    outputOperands.add(new Entry("BLEZ", List.of()));
    outputOperands.add(new Entry("BLTZ", List.of()));
    outputOperands.add(new Entry("BNEZ", List.of()));
    outputOperands.add(new Entry("CALL", List.of()));
    outputOperands.add(new Entry("J", List.of()));
    outputOperands.add(new Entry("LI", List.of("X:$rd")));
    outputOperands.add(new Entry("LLA", List.of("X:$rd")));
    outputOperands.add(new Entry("MV", List.of("X:$rd")));
    outputOperands.add(new Entry("NEG", List.of("X:$rd")));
    outputOperands.add(new Entry("NOP", List.of()));
    outputOperands.add(new Entry("NOT", List.of("X:$rd")));
    outputOperands.add(new Entry("RET", List.of()));
    outputOperands.add(new Entry("SGTZ", List.of("X:$rd")));
    outputOperands.add(new Entry("SLTZ", List.of("X:$rd")));
    outputOperands.add(new Entry("SNEZ", List.of("X:$rd")));
    outputOperands.add(new Entry("TAIL", List.of()));
  }

  @TestFactory
  List<DynamicTest> shouldHaveInputOperands()
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateTableGenPseudoInstructionRecordPass.class.getName()));
    var passManager = setup.passManager();

    // When
    var pseudoInstructionRecords = (List<TableGenPseudoInstruction>) passManager.getPassResults()
        .lastResultOf(GenerateTableGenPseudoInstructionRecordPass.class);

    // Then
    var actual = pseudoInstructionRecords.stream()
        .map(x -> new Entry(x.getName(),
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
        new PassKey(GenerateTableGenPseudoInstructionRecordPass.class.getName()));
    var passManager = setup.passManager();

    // When
    var pseudoInstructionRecords = (List<TableGenPseudoInstruction>) passManager.getPassResults()
        .lastResultOf(GenerateTableGenPseudoInstructionRecordPass.class);

    // Then
    var actual = pseudoInstructionRecords.stream()
        .map(x -> new Entry(x.getName(),
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
