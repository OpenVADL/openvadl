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

package vadl.gcb.riscv.riscv64.passes;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.gcb.AbstractGcbTest;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.RelocationKindCtx;
import vadl.gcb.passes.relocation.model.CompilerRelocation;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;

class DetermineRelocationTypeForFieldPassTest extends AbstractGcbTest {

  private static Stream<Arguments> interestingInstructions() {
    return Stream.of(
        Arguments.of("BEQ", "imm", CompilerRelocation.Kind.RELATIVE),
        Arguments.of("BNE", "imm", CompilerRelocation.Kind.RELATIVE),
        Arguments.of("BGE", "imm", CompilerRelocation.Kind.RELATIVE),
        Arguments.of("BLT", "imm", CompilerRelocation.Kind.RELATIVE),
        Arguments.of("BLTU", "imm", CompilerRelocation.Kind.RELATIVE)
    );
  }

  /**
   * We are using {@link #interestingInstructions()} and extracting only the instruction name
   * so Intellij only shows the name as test parameter. Otherwise, it will show all three, however,
   * field and compiler relocation kind are irrelevant.
   */
  private static Stream<Arguments> interestingInstructionsForRegisterTest() {
    return interestingInstructions()
        .map(arguments -> Arguments.of(arguments.get()[0]));
  }

  @MethodSource(value = "interestingInstructions")
  @ParameterizedTest
  void shouldDetermineCompilerRelocationKind(String instructionName,
                                             String field,
                                             CompilerRelocation.Kind kind)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(DetermineRelocationTypeForFieldPassTest.class.getName()));
    var passManager = setup.passManager();

    var container =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passManager.getPassResults()
            .lastResultOf(IdentifyFieldUsagePass.class);

    // When
    // Pass no direct result

    // Then
    var instruction = getInstrByName(instructionName, setup.specification());
    Assertions.assertNotNull(instruction);

    var ctx = instruction.expectExtension(RelocationKindCtx.class);
    Assertions.assertNotNull(ctx);

    var immediates = container.getImmediates(instruction);
    Assertions.assertNotNull(immediates);

    var formatField = getImmediate(field, immediates);
    Assertions.assertTrue(formatField.isPresent(), "Tested field is not an immediate field");

    // The actual test is here.
    Assertions.assertTrue(ctx.getFieldToKind().containsKey(formatField.get()),
        "Tested field is not a relocation field");
    Assertions.assertEquals(kind, ctx.getFieldToKind().get(formatField.get()));
  }

  @MethodSource(value = "interestingInstructionsForRegisterTest")
  @ParameterizedTest
  void shouldNotDetermineCompilerRelocationKindForRegisters(String instructionName)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(DetermineRelocationTypeForFieldPassTest.class.getName()));
    var passManager = setup.passManager();

    var container =
        (IdentifyFieldUsagePass.ImmediateDetectionContainer) passManager.getPassResults()
            .lastResultOf(IdentifyFieldUsagePass.class);

    // When
    // Pass no direct result

    // Then
    var instruction = getInstrByName(instructionName, setup.specification());
    Assertions.assertNotNull(instruction);

    var ctx = instruction.expectExtension(RelocationKindCtx.class);
    Assertions.assertNotNull(ctx);

    var registers = container.getRegisterUsages(instruction);
    Assertions.assertNotNull(registers);

    // The actual test is here.
    for (var register : registers.keySet()) {
      Assertions.assertFalse(ctx.getFieldToKind().containsKey(register),
          "Register has relocation kind but must not");
    }
  }

}