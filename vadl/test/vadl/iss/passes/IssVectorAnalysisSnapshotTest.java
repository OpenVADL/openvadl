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

package vadl.iss.passes;

import static vadl.TestUtils.assertEqualsFileLines;
import static vadl.iss.passes.TcgPassUtils.instrInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.AssertionFailedError;
import vadl.AbstractTest;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.common.planning.IssExecStrategyPass;
import vadl.iss.passes.extensions.InstrExecPlan;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Snapshot tests for the current instruction path selection plus region-based vector analysis.
 *
 * <p>To update snapshots:
 * {@code UPDATE_SNAPSHOTS=true ./gradlew test --tests vadl.iss.passes.IssVectorAnalysisSnapshotTest}
 * </p>
 */
class IssVectorAnalysisSnapshotTest extends AbstractTest {

  private static final Path SNAPSHOT_ROOT = Path.of("test/resources/snapshots/iss/vector-analysis");

  @TestFactory
  Stream<DynamicTest> snapshots() {
    return fixtures().stream()
        .map(fixture -> DynamicTest.dynamicTest(fixture.specPath(), () -> runSnapshot(fixture)));
  }

  private void runSnapshot(Fixture fixture) throws IOException, DuplicatedPassKeyException {
    var viam = analyze(fixture.specPath());
    var actual = renderSnapshot(fixture.specPath(), viam);

    if (System.getenv("UPDATE_SNAPSHOTS") != null) {
      Files.createDirectories(fixture.snapshotPath().getParent());
      Files.writeString(fixture.snapshotPath(), actual);
      return;
    }

    if (!Files.exists(fixture.snapshotPath())) {
      throw new AssertionFailedError("Missing snapshot file: " + fixture.snapshotPath());
    }
    assertEqualsFileLines(fixture.snapshotPath(), actual);
  }

  private Specification analyze(String specPath) throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.iss(config()).untilFirst(IssExecStrategyPass.class)
    ).specification();
  }

  private IssConfiguration config() {
    return new IssConfiguration(
        new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE)
    );
  }

  private String renderSnapshot(String specPath, Specification viam) {
    var lines = viam.isa().orElseThrow().ownInstructions().stream()
        .sorted((lhs, rhs) -> lhs.identifier().name().compareTo(rhs.identifier().name()))
        .map(this::renderInstruction)
        .toList();
    return String.join("\n", Stream.concat(
        Stream.of(
            "# Vector analysis snapshot",
            "# spec: " + specPath,
            ""
        ),
        lines.stream()
    ).toList()) + "\n";
  }

  private String renderInstruction(Instruction instruction) {
    var executionPlan = instrInfo(instruction).executionPlan();
    if (executionPlan == null) {
      throw new AssertionFailedError(
          "Missing execution plan for instruction " + instruction.identifier().name()
      );
    }
    return instruction.identifier().name()
        + " | " + pathOf(executionPlan)
        + " | " + regionsOf(executionPlan);
  }

  private String pathOf(InstrExecPlan executionPlan) {
    if (executionPlan.usesNormalTcgPath()) {
      return "normal-tcg";
    }
    return "helper-call";
  }

  private String regionsOf(InstrExecPlan executionPlan) {
    if (executionPlan.directGvecRegions().isEmpty()) {
      return "-";
    }
    return executionPlan.directGvecRegions().stream()
        .sorted((lhs, rhs) -> lhs.region().regionId().compareTo(rhs.region().regionId()))
        .map(region -> region.region().regionId() + ":" + outcomeOf(region))
        .collect(Collectors.joining(";"));
  }

  private String outcomeOf(InstrExecPlan.DirectGvecSupport region) {
    if (region.isViable()) {
      return "gvec";
    }
    if (region.issues().isEmpty()) {
      return "rejected";
    }
    return "rejected[" + region.issues().stream()
        .map(InstrExecPlan.PlanningIssue::code)
        .distinct()
        .collect(Collectors.joining(",")) + "]";
  }

  private List<Fixture> fixtures() {
    return List.of(
        new Fixture("sys/aarch64/vprocessor.vadl", SNAPSHOT_ROOT.resolve("vprocessor.txt")),
        new Fixture("sys/risc-v/rv64v.vadl", SNAPSHOT_ROOT.resolve("rv64v.txt")),
        new Fixture("sys/vectorbench/vectorbench64.vadl",
            SNAPSHOT_ROOT.resolve("vectorbench64.txt"))
    );
  }

  private record Fixture(String specPath, Path snapshotPath) {
  }
}
