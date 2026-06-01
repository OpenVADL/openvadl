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

package vadl.iss.codegen;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.configuration.IssConfiguration;
import vadl.iss.template.target.EmitIssCpuHeaderPass;
import vadl.iss.template.target.EmitIssTranslateCPass;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class IssTbStateCCodeSnapshotTest extends AbstractTest {

  private static final Path SNAPSHOT_ROOT = Path.of("test/resources/snapshots/iss/tb-state");

  private Pattern initDisasContextPattern = Pattern.compile(
      "^[^\n]*test_tr_init_disas_context[^}]*}$", Pattern.MULTILINE | Pattern.DOTALL);
  private Pattern getTbCpuPattern = Pattern.compile(
      "^[^\n]*cpu_get_tb_cpu_state[^}]*}$", Pattern.MULTILINE | Pattern.DOTALL);

  @TestFactory
  Stream<DynamicTest> tests() {
    return fixtures().stream()
        .map(fixture -> DynamicTest.dynamicTest(fixture.specPath(), () -> runSnapshot(fixture)));
  }

  private void runSnapshot(Fixture fixture) throws IOException, DuplicatedPassKeyException {
    var config = new IssConfiguration(getConfiguration(false));
    var passResults = setupPassManagerAndRunSpec(
        fixture.specPath,
        PassOrders.iss(config).untilFirst(EmitIssTranslateCPass.class)
    ).passManager().getPassResults();

    var translateCFile = ((AbstractTemplateRenderingPass.Result) passResults
        .lastResultOf(EmitIssTranslateCPass.class)).emittedFile();
    var cpuHeaderFile = ((AbstractTemplateRenderingPass.Result) passResults
        .lastResultOf(EmitIssCpuHeaderPass.class)).emittedFile();

    var translateCStr = FileUtils.readFileToString(translateCFile.toFile(), "UTF-8");
    var cpuHeaderStr = FileUtils.readFileToString(cpuHeaderFile.toFile(), "UTF-8");

    var initDisasContextMatcher = initDisasContextPattern.matcher(translateCStr);
    assertTrue(initDisasContextMatcher.find());
    var initDisasContext = initDisasContextMatcher.group();

    var getTbCpuStateMatcher = getTbCpuPattern.matcher(cpuHeaderStr);
    assertTrue(getTbCpuStateMatcher.find());
    var getTbCpuState = getTbCpuStateMatcher.group();

    checkAllLinesPresent(fixture.translateCPath, initDisasContext);
    checkAllLinesPresent(fixture.cpuHPath, getTbCpuState);
  }

  private void checkAllLinesPresent(Path expectedLines, String actual) throws IOException {
    assertAll(Files.lines(expectedLines).map(
        line -> () -> assertThat(actual, containsString(line))));
  }

  private List<Fixture> fixtures() {
    return List.of(
        new Fixture(
            "iss/tb-state/tb_state_spec.vadl",
            SNAPSHOT_ROOT.resolve("tb_state_spec_translate_c.txt"),
            SNAPSHOT_ROOT.resolve("tb_state_spec_cpu_h.txt")
        )
    );
  }

  private record Fixture(String specPath, Path translateCPath, Path cpuHPath) { }
}
