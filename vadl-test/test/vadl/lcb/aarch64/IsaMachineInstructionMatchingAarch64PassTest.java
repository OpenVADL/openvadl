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

package vadl.lcb.aarch64;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.gcb.passes.MachineInstructionCtx;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.pipeline.LcbGcbPassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Definition;

public class IsaMachineInstructionMatchingAarch64PassTest extends AbstractLcbTest {

  private static Stream<Arguments> getExpectedMatchings() {
    return Stream.of(
        Arguments.of(List.of("B", "B_AL", "B_NV"), MachineInstructionLabel.J,
            Optional.of(DataType.bits(64))),
        Arguments.of(
            List.of("B_EQ"),
            MachineInstructionLabel.BEQ_BY_STATUS_REGISTER,
            Optional.empty()),
        Arguments.of(List.of("B_NE"), MachineInstructionLabel.BNEQ_BY_STATUS_REGISTER,
            Optional.empty()),
        Arguments.of(List.of("B_GE"), MachineInstructionLabel.BSGEQ_BY_STATUS_REGISTER,
            Optional.empty()),
        Arguments.of(List.of("B_GT"), MachineInstructionLabel.BSGTH_BY_STATUS_REGISTER,
            Optional.empty()),
        Arguments.of(List.of("B_LT"), MachineInstructionLabel.BSLTH_BY_STATUS_REGISTER,
            Optional.empty()),
        Arguments.of(List.of("B_LE"), MachineInstructionLabel.BSLEQ_BY_STATUS_REGISTER,
            Optional.empty()),
        Arguments.of(List.of("CBZW", "CBZX", "TBZ"), MachineInstructionLabel.BEQ,
            Optional.empty()),
        Arguments.of(List.of("CBNZW", "CBNZX", "TBNZ"), MachineInstructionLabel.BNEQ,
            Optional.empty()),
        Arguments.of(List.of("SUBXS", "SUBXSSXTX", "SUBXSUXTX"),
            MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_64,
            Optional.empty()),
        Arguments.of(List.of("SUBWS", "SUBWSSXTX", "SUBWSUXTX"),
            MachineInstructionLabel.SUB_RR_WITH_STATUS_REGISTER_32,
            Optional.empty()),
        Arguments.of(List.of("CSELEQX"),
            MachineInstructionLabel.CSEL_EQ_I64,
            Optional.empty()),
        Arguments.of(List.of("CSELNEX"),
            MachineInstructionLabel.CSEL_NEQ_I64,
            Optional.empty()),
        Arguments.of(List.of("ORRW", "ORRX"),
            MachineInstructionLabel.OR,
            Optional.empty()),
        Arguments.of(List.of("ADDX", "ADDXSXTX", "ADDXUXTX"),
            MachineInstructionLabel.ADD_64,
            Optional.empty())
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
        "sys/aarch64/aarch64-abi.vadl",
        LcbGcbPassOrders.lcb(config)
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
        "sys/aarch64/aarch64-abi.vadl",
        LcbGcbPassOrders.lcb(config)
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
