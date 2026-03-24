// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss;

import static vadl.iss.CosimTestUtils.writeTestSuiteConfigYaml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.testcontainers.utility.MountableFile;
import vadl.DockerImage;

public abstract class CosimInstrTest extends CosimTest {

  public abstract int getTestPerInstruction();

  public abstract String getVadlSpec();

  public abstract String getCosimConfigFileName();

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      Function<Integer, CosimTestUtils.TestCase>... generators) throws IOException {
    return runTestsWith(getTestPerInstruction(), List.of(generators));
  }

  protected final Stream<DynamicTest> runTestsWith(
      int runs,
      List<Function<Integer, CosimTestUtils.TestCase>> generators) throws IOException {
    if (generators.isEmpty()) {
      throw new IllegalArgumentException("No generators specified");
    }
    var image = generateIssSimulator(getVadlSpec());
    var testCases = generators.stream()
        .flatMap(genFunc -> IntStream.range(0, runs)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }

  protected Stream<DynamicTest> runQemuInstrTests(DockerImage image,
                                                  Collection<CosimTestUtils.TestCase> testCases)
      throws IOException {

    var testConfig =
        new CosimTestUtils.TestConfig("/cosim_configs/" + getCosimConfigFileName(), testCases);

    // resolve file that contains all test specifications.
    // it is a yaml file that gets mapped to `/work/test-suite.yaml` of the container.
    var testDirectory = getTestDirectory();
    var testSuiteYaml = testDirectory.resolve("test-suite.yaml").toFile();
    var resultDirectory = testDirectory.resolve("results").toAbsolutePath();
    // write the test cases to this yaml file
    writeTestSuiteConfigYaml(testConfig, testSuiteYaml);
    // run the container and copy the test cases into the container
    // and after execution, copy the results from the container
    runContainer(image, container -> container
            .withCopyToContainer(MountableFile.forHostPath(testSuiteYaml.getPath()),
                "/work/test-suite.yaml"),
        container -> copyPathFromContainer(container, "/work/results/", resultDirectory)
    );

    Map<String, File> resultFiles = new HashMap<>();
    try (var walkStream = java.nio.file.Files.walk(resultDirectory)) {
      walkStream
          .filter(java.nio.file.Files::isRegularFile)
          .forEach(path -> {
            var id = path.getFileName().toString().substring(7);
            resultFiles.put(id, path.toFile());
          });
    } catch (Exception e) {
      Assertions.fail("Failed to load test results.", e);
    }

    return testCases.stream()
        .map(e -> DynamicTest.dynamicTest(e.id(),
            () -> {
              if (!resultFiles.containsKey(e.id())) {
                Assertions.fail("Result file is missing for test: " + e.id());
              }
              var file = resultFiles.get(e.id());
              var parsed = CosimTestUtils.yamlToTestResult(file);
              if (!parsed.passed()) {
                var sb = new StringBuilder();
                appendFailureDetails(sb, e, parsed);
                Assertions.fail(sb.toString());
              }
            }
        ));
  }

  private void appendFailureDetails(StringBuilder sb,
                                    CosimTestUtils.TestCase testCase,
                                    CosimTestUtils.TestResult result) {
    sb.append("Test failed for test: ").append(testCase.id()).append("\n");
    sb.append("Assembly core:\n").append(testCase.asmCore()).append("\n");
    if (!result.diffs().isEmpty()) {
      sb.append("Differences:\n");
      var clients = result.diffContext();
      for (var diff : result.diffs()) {
        sb.append("- ").append(diff.description()).append(":\n");
        int maxNameLen = clients.stream()
            .map(c -> c.clientName() == null ? "<unknown>" : c.clientName())
            .mapToInt(String::length)
            .max()
            .orElse(0);
        for (int i = 0; i < diff.values().size(); i++) {
          var name = i < clients.size() && clients.get(i).clientName() != null
              ? clients.get(i).clientName() : "<unknown>";
          var pad = " ".repeat(maxNameLen - name.length());
          sb.append("  ").append(name).append(pad).append(" ").append(diff.values().get(i))
              .append("\n");
        }
      }
    }
  }

}
