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

package vadl.iss;

import static vadl.iss.IssTestUtils.writeTestSuiteConfigYaml;
import static vadl.iss.IssTestUtils.yamlToTestResult;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import org.testcontainers.utility.MountableFile;

public abstract class IssInstrTest extends QemuIssTest {

  public record Tool(
      String path,
      String args
  ) {
    public Map<String, String> toMap() {
      return Map.of(
          "path", path,
          "args", args
      );
    }
  }

  abstract int getTestPerInstruction();

  abstract String getVadlSpec();

  abstract AsmTestBuilder getBuilder(String testNamePrefix, int id);

  abstract Map<String, String> gdbRegMap();

  abstract Tool simulator();

  abstract Tool reference();

  abstract Tool compiler();

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      Function<Integer, IssTestUtils.TestCase>... generators) throws IOException {
    return runTestsWith(getTestPerInstruction(), generators);
  }

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      int runs,
      Function<Integer, IssTestUtils.TestCase>... generators) throws IOException {
    var image = generateIssSimulator(getVadlSpec());
    var testCases = Stream.of(generators)
        .flatMap(genFunc -> IntStream.range(0, runs)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }

  /**
   * Runs a QEMU instr test on the given image with the given test cases.
   *
   * @param image     the QEMU image to run the tests on
   * @param testCases the test cases passed to the container which runs the tests
   * @return the test result as DynamicTests as integration with JUnit
   */
  protected Stream<DynamicTest> runQemuInstrTests(ImageFromDockerfile image,
                                                  Collection<IssTestUtils.TestCase> testCases)
      throws IOException {

    var statePlugin = "/qemu/build/tests/tcg/plugins/libendstate.so";

    var testConfig = new IssTestUtils.TestConfig(
        simulator().toMap(),
        reference().toMap(),
        compiler().toMap(),
        statePlugin,
        testCases,
        gdbRegMap()
    );

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


    List<IssTestUtils.TestResult> testResults = List.of();

    try (var walkStream = java.nio.file.Files.walk(resultDirectory)) {
      // parse the result yaml files into a list of TestResults
      testResults = walkStream
          .filter(file -> file.toString().endsWith(".yaml"))
          .map(file -> yamlToTestResult(file.toFile()))
          .toList();
    } catch (Exception e) {
      Assertions.fail("Failed to load test results.", e);
    }

    // just for fast access later
    var specIds = testResults.stream()
        .map(IssTestUtils.TestResult::id)
        .collect(Collectors.toSet());
    var testCaseMap = testCases.stream()
        .collect(Collectors.toMap(IssTestUtils.TestCase::id, s -> s));

    // produce DynamicTests for all parsed test results.
    // these will be listed in the JUnit test report
    var normalTestResultDynamicTests = testResults.stream()
        .map(e -> DynamicTest.dynamicTest(e.id(),
            () -> {
              var testSpec = testCaseMap.get(e.id());
              var success = IssTestUtils.TestResult.Status.PASS == e.status();
              System.out.println("----------------");
              System.out.println("Test " + e.id());
              System.out.println("ASM: \n" + testSpec.asmCore());
              System.out.println("\nRan stages: " + e.completedStages());
              System.out.println("Register tests: \n" + e.regTests());
              System.out.println("Duration: " + e.duration());

              if (!success) {
                for (var log : e.simLogs().entrySet()) {
                  System.out.println("[SIM] Logs of " + log.getKey() + ": ");
                  log.getValue().stream().map((l) -> "- " + l)
                      .forEach(System.out::println);
                }
                for (var log : e.simLogs().entrySet()) {
                  System.out.println("[REF] Logs of " + log.getKey() + ": ");
                  log.getValue().stream().map((l) -> "- " + l)
                      .forEach(System.out::println);
                }
              }
              System.out.println("----------------");

              Assertions.assertEquals(IssTestUtils.TestResult.Status.PASS, e.status(),
                  String.join(",\n\t", e.errors()));
            }
        ));

    // produces dynamic tests for all cases where no result was found
    var notFoundResultDynamicTests = testCases.stream()
        .filter(c -> !specIds.contains(c.id()))
        .map(c -> DynamicTest.dynamicTest("Find result of " + c.id(),
            () -> Assertions.fail("No result found for test " + c.id())
        ));

    // return stream of all dynamic test cases
    return Streams.concat(
        normalTestResultDynamicTests,
        notFoundResultDynamicTests
    );
  }

}
