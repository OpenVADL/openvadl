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

import static vadl.gcb.passes.InstructionIntrinsicAttributesCtx.Attribute.NoMem;
import static vadl.gcb.passes.InstructionIntrinsicAttributesCtx.Attribute.Speculatable;
import static vadl.gcb.passes.InstructionIntrinsicAttributesCtx.Attribute.WillReturn;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.gcb.AbstractGcbTest;
import vadl.gcb.passes.DetermineIntrinsicAttributesPass;
import vadl.gcb.passes.InstructionIntrinsicAttributesCtx;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;

public class DetermineIntrinsicAttributesPassTest extends AbstractGcbTest {
  public static Stream<Arguments> expected() {
    return Stream.of(
        Arguments.of("ADD", List.of(NoMem, WillReturn, Speculatable)),
        Arguments.of("SUB", List.of(NoMem, WillReturn, Speculatable)),
        Arguments.of("MUL", List.of(NoMem, WillReturn, Speculatable))
    );
  }

  @MethodSource(value = "expected")
  @ParameterizedTest
  void shouldDetect(String instructionName,
                    List<InstructionIntrinsicAttributesCtx.Attribute> attrs)
      throws DuplicatedPassKeyException, IOException {
    // Given
    var setup = runGcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(DetermineIntrinsicAttributesPassTest.class.getName()));
    var passManager = setup.passManager();

    // When
    var result =
        ((Map<Instruction, List<InstructionIntrinsicAttributesCtx.Attribute>>) passManager.getPassResults()
            .lastResultOf(DetermineIntrinsicAttributesPass.class)).entrySet().stream().collect(
            Collectors.toMap(x -> x.getKey().simpleName(), Map.Entry::getValue));

    // Then
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.get(instructionName));
    Assertions.assertEquals(result.get(instructionName), attrs);
  }
}