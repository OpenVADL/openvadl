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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.gcb.passes.IsaMachineInstructionMatchingPass;
import vadl.gcb.passes.MachineInstructionCtx;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.AbstractLcbTest;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Definition;

public class IsaMachineInstructionMatchingRiscv64PassTest extends AbstractLcbTest {

  private static Stream<Arguments> getExpectedMatchings() {
    return Stream.of(
        Arguments.of(List.of("ADD"), MachineInstructionLabel.ADD_64,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("ADDI"), MachineInstructionLabel.ADDI_64,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("BEQ"), MachineInstructionLabel.BEQ, Optional.empty()),
        Arguments.of(List.of("BNE"), MachineInstructionLabel.BNEQ, Optional.empty()),
        Arguments.of(List.of("BGE"), MachineInstructionLabel.BSGEQ, Optional.empty()),
        Arguments.of(List.of("BGEU"), MachineInstructionLabel.BUGEQ, Optional.empty()),
        Arguments.of(List.of("BLT"), MachineInstructionLabel.BSLTH, Optional.empty()),
        Arguments.of(List.of("BLTU"), MachineInstructionLabel.BULTH, Optional.empty()),
        Arguments.of(List.of("AND", "ANDI"), MachineInstructionLabel.AND,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SUB", "SUBW"), MachineInstructionLabel.SUB,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("OR"), MachineInstructionLabel.OR,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("ORI"), MachineInstructionLabel.ORI,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("XOR"), MachineInstructionLabel.XOR,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("XORI"), MachineInstructionLabel.XORI,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("MUL", "MULW"), MachineInstructionLabel.MUL,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("MULHU"), MachineInstructionLabel.MULHU,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("MULH"), MachineInstructionLabel.MULHS,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("DIV", "DIVW"), MachineInstructionLabel.SDIV,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("DIVU", "DIVUW"), MachineInstructionLabel.UDIV,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("REMU", "REMUW"), MachineInstructionLabel.UMOD,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("REM", "REMW"), MachineInstructionLabel.SMOD,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SLL", "SLLW"), MachineInstructionLabel.SLL,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SLLI"), MachineInstructionLabel.SLLI,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SRL", "SRLW"), MachineInstructionLabel.SRL,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SLT"), MachineInstructionLabel.LTS,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SLTU"), MachineInstructionLabel.LTU,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SLTI"), MachineInstructionLabel.LTI,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SLTIU"), MachineInstructionLabel.LTIU,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("LB", "LBU", "LD", "LH", "LHU", "LW", "LWU"),
            MachineInstructionLabel.LOAD_MEM, Optional.of(DataType.bits(64))),
        Arguments.of(List.of("SB", "SD", "SH", "SW"), MachineInstructionLabel.STORE_MEM,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("JALR"), MachineInstructionLabel.JALR,
            Optional.of(DataType.bits(64))),
        Arguments.of(List.of("JAL"), MachineInstructionLabel.JAL,
            Optional.of(DataType.bits(64)))
    );
  }

  @ParameterizedTest
  @MethodSource("getExpectedMatchings")
  void shouldFindMatchings(List<String> expectedInstructionName,
                           MachineInstructionLabel label,
                           Optional<BitsType> dataType)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "sys/risc-v/rv64im.vadl",
        PassOrders.lcb(config)
            .untilFirst(IsaMachineInstructionMatchingPass.class)
    );
    var passManager = setup.passManager();

    // When
    var matchings =
        ((IsaMachineInstructionMatchingPass.Result) passManager.getPassResults()
            .lastResultOf(IsaMachineInstructionMatchingPass.class)).labels();

    // Then
    Assertions.assertNotNull(matchings);
    Assertions.assertFalse(matchings.isEmpty());
    Assertions.assertNotNull(matchings.get(label));
    var result = matchings.get(label).stream().map(Definition::simpleName).sorted().toList();
    assertEquals(expectedInstructionName.stream().sorted().toList(), result);
  }


  @ParameterizedTest
  @MethodSource("getExpectedMatchings")
  void shouldFindMatchingsByExtension(List<String> expectedInstructionName,
                                      MachineInstructionLabel label,
                                      Optional<BitsType> dataType)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "sys/risc-v/rv64im.vadl",
        PassOrders.lcb(config)
            .untilFirst(IsaMachineInstructionMatchingPass.class)
    );

    // When
    for (var instructionName : expectedInstructionName) {
      var instruction = setup.specification().isa().get().ownInstructions()
          .stream().filter(x -> x.identifier.simpleName().equals(instructionName))
          .findFirst()
          .get();

      var ctx = instruction.extension(MachineInstructionCtx.class);

      // Then
      Assertions.assertNotNull(ctx);
      Assertions.assertEquals(label, ctx.label());
      if (dataType.isPresent()) {
        Assertions.assertEquals(dataType.get(), ctx.type().get());
      } else {
        Assertions.assertTrue(ctx.type().isEmpty());
      }
    }
  }
}