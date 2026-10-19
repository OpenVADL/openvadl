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

package vadl.lcb.riscv.riscv32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.gcb.passes.MachineInstructionCtx;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.database.Database;
import vadl.lcb.passes.isaMatching.database.Query;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Definition;
import vadl.viam.Instruction;

/**
 * Checks that instructions whose behavior matches multiple {@link MachineInstructionLabel}s
 * get all of them, and that they can be found by every one of their labels.
 */
public class IsaMachineInstructionMultipleLabelsRiscv32PassTest extends AbstractLcbTest {

  private TestSetup setup;
  private IsaMachineInstructionMatchingPass.Result result;

  @BeforeEach
  void setUp() throws IOException, DuplicatedPassKeyException {
    var config = getConfiguration(false);
    setup = setupPassManagerAndRunSpec(
        "lcb/riscv32_multiple_labels.vadl",
        PassOrders.lcb(config)
            .untilFirst(IsaMachineInstructionMatchingPass.class)
    );
    result = (IsaMachineInstructionMatchingPass.Result) setup.passManager().getPassResults()
        .lastResultOf(IsaMachineInstructionMatchingPass.class);
  }

  static Stream<Arguments> getExpectedLabels() {
    return Stream.of(
        Arguments.of("UBFIZ",
            List.of(MachineInstructionLabel.AND, MachineInstructionLabel.SLLI))
    );
  }

  @ParameterizedTest
  @MethodSource("getExpectedLabels")
  void shouldAssignAllLabels(String instructionName, List<MachineInstructionLabel> labels) {
    var ctx = instruction(instructionName).extension(MachineInstructionCtx.class);

    assertNotNull(ctx);
    assertEquals(labels, ctx.labels());
    assertEquals(labels.getFirst(), ctx.label());
  }

  @ParameterizedTest
  @MethodSource("getExpectedLabels")
  void shouldListInstructionUnderEveryLabel(String instructionName,
                                            List<MachineInstructionLabel> labels) {
    for (var label : labels) {
      var names = result.labels().get(label).stream().map(Definition::simpleName).toList();
      assertTrue(names.contains(instructionName),
          instructionName + " is not listed under " + label + ": " + names);
    }
  }

  @ParameterizedTest
  @MethodSource("getExpectedLabels")
  void shouldUsePrimaryLabelForReverseLookup(String instructionName,
                                             List<MachineInstructionLabel> labels) {
    assertEquals(labels.getFirst(), result.reverse().get(instruction(instructionName)));
  }

  @ParameterizedTest
  @MethodSource("getExpectedLabels")
  void databaseShouldFindInstructionByEveryLabel(String instructionName,
                                                 List<MachineInstructionLabel> labels) {
    var database = new Database(result);

    for (var label : labels) {
      var names = database.run(new Query.Builder().machineInstructionLabel(label).build())
          .machineInstructions().stream().map(Definition::simpleName).toList();
      assertTrue(names.contains(instructionName),
          "Query for " + label + " does not find " + instructionName + ": " + names);
    }
  }

  @Test
  void databaseShouldFindSecondaryLabelNextToRegularInstruction() {
    var database = new Database(result);

    var names = database.run(
            new Query.Builder().machineInstructionLabel(MachineInstructionLabel.SLLI).build())
        .machineInstructions().stream().map(Definition::simpleName).sorted().toList();

    assertEquals(List.of("SLLI", "UBFIZ"), names);
  }

  @Test
  void databaseShouldReturnInstructionOnlyOnceWhenMatchedByMultipleLabels() {
    var database = new Database(result);

    var names = database.run(new Query.Builder()
            .machineInstructionLabels(
                List.of(MachineInstructionLabel.AND, MachineInstructionLabel.SLLI))
            .build())
        .machineInstructions().stream().map(Definition::simpleName).sorted().toList();

    assertEquals(List.of("SLLI", "UBFIZ"), names);
  }

  private Instruction instruction(String name) {
    return setup.specification().isa().orElseThrow().ownInstructions().stream()
        .filter(instruction -> instruction.simpleName().equals(name))
        .findFirst()
        .orElseThrow();
  }
}
