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

import static vadl.iss.IssTestUtils.writeTestSuiteConfigYaml;
import static vadl.iss.IssTestUtils.yamlToTestResult;
import static vadl.iss.IssTestUtils.yamlToTestResultHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.testcontainers.utility.MountableFile;
import vadl.DockerImage;

@SuppressWarnings("OverloadMethodsDeclarationOrder")
public abstract class IssInstrTest extends QemuIssTest {
  public record InstructionTestGroup(
      String name,
      List<IssTestUtils.TestCase> testCases
  ) {
  }

  private static final Map<Class<? extends IssInstrTest>, InstructionBatchState>
      INSTRUCTION_BATCHES =
      new ConcurrentHashMap<>();

  private static final class InstructionBatchState {
    private List<InstructionTestGroup> instructionGroups;
    private Map<String, String> failures;
  }

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

  public abstract int getTestPerInstruction();

  public abstract String getVadlSpec();

  public abstract AsmTestBuilder getBuilder(String testNamePrefix, int id);

  public abstract Map<String, String> gdbRegMap();

  public abstract Tool simulator();

  public abstract Tool reference();

  public abstract Tool compiler();


  protected final Stream<DynamicTest> runTest(
      IssTestUtils.TestCase test) throws IOException {
    return runTestsWith(1, (i) -> test);
  }

  /**
   * Initializes a cached batched ISS run for the current test class.
   *
   * <p>Use this when the suite already has an explicit grouping structure. The batch is executed
   * at most once per concrete subclass; repeated calls are cheap and simply reuse the cached
   * results.</p>
   */
  protected final void initializeInstructionBatch(
      List<InstructionTestGroup> instructionGroups)
      throws IOException {
    initializeInstructionBatch(() -> instructionGroups);
  }

  /**
   * Lazy variant of {@link #initializeInstructionBatch(List)}.
   *
   * <p>The supplier is only evaluated if the current subclass has not initialized its cached batch
   * yet. This is useful when both a setup test and a {@code @TestFactory} call into the same
   * initialization path.</p>
   */
  protected final void initializeInstructionBatch(
      Supplier<List<InstructionTestGroup>> instructionGroupsSupplier)
      throws IOException {
    var state =
        INSTRUCTION_BATCHES.computeIfAbsent(getClass(), ignored -> new InstructionBatchState());
    synchronized (state) {
      if (state.instructionGroups != null && state.failures != null) {
        return;
      }
      state.instructionGroups = List.copyOf(instructionGroupsSupplier.get());
      var testCases = state.instructionGroups.stream()
          .flatMap(group -> group.testCases().stream())
          .toList();
      state.failures =
          runQemuInstrTestsAndCollectFailures(generateIssSimulator(getVadlSpec()), testCases);
    }
  }

  /**
   * Initializes a cached batched ISS run from a flat testcase list and a grouping function.
   *
   * <p>This is the most common entry point for instruction suites: subclasses build all testcases
   * once, then provide a function that maps each testcase to the container name that should be
   * shown in JUnit.</p>
   */
  protected final void initializeInstructionBatchFromTestCases(
      List<IssTestUtils.TestCase> testCases,
      Function<IssTestUtils.TestCase, String> groupName)
      throws IOException {
    initializeInstructionBatchFromTestCases(() -> testCases, groupName);
  }

  protected final void initializeInstructionBatchFromTestCases(
      Supplier<List<IssTestUtils.TestCase>> testCasesSupplier,
      Function<IssTestUtils.TestCase, String> groupName)
      throws IOException {
    initializeInstructionBatch(() -> {
      var groups = new LinkedHashMap<String, List<IssTestUtils.TestCase>>();
      for (var testCase : testCasesSupplier.get()) {
        groups.computeIfAbsent(groupName.apply(testCase), ignored -> new ArrayList<>())
            .add(testCase);
      }
      return groups.entrySet().stream()
          .map(entry -> new InstructionTestGroup(entry.getKey(), List.copyOf(entry.getValue())))
          .toList();
    });
  }

  /**
   * Builds one {@link DynamicContainer} per previously initialized instruction group.
   *
   * <p>Call one of the {@code initializeInstructionBatch*} methods first. Each container contains
   * one dynamic test per testcase, and each dynamic test simply replays the cached pass/fail result
   * of the batched ISS run.</p>
   */
  protected final Stream<DynamicNode> buildInstructionTestContainers() {
    var state = requireInstructionBatchState();
    return state.instructionGroups.stream()
        .map(group -> DynamicContainer.dynamicContainer(
            group.name(),
            buildDynamicTests(group.testCases(), state.failures)));
  }

  protected final Map<String, String> getInstructionBatchFailures() {
    return requireInstructionBatchState().failures;
  }

  protected final List<InstructionTestGroup> getInstructionBatchGroups() {
    return requireInstructionBatchState().instructionGroups;
  }

  private InstructionBatchState requireInstructionBatchState() {
    var state = INSTRUCTION_BATCHES.get(getClass());
    if (state == null || state.instructionGroups == null || state.failures == null) {
      throw new IllegalStateException(
          "Instruction batch not initialized for " + getClass().getSimpleName());
    }
    return state;
  }

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      Function<Integer, IssTestUtils.TestCase>... generators) throws IOException {
    return runTestsWith(getTestPerInstruction(), List.of(generators));
  }

  /**
   * Builds testcase specifications without executing them.
   *
   * <p>Prefer this over {@code runTestsWith(...)} when a suite wants to batch all testcases into a
   * single ISS run and report the results later via {@code DynamicContainer}s or cached dynamic
   * tests.</p>
   */
  @SafeVarargs
  protected final List<IssTestUtils.TestCase> buildTestsWith(
      Function<Integer, IssTestUtils.TestCase>... generators) {
    return buildTestsWith(getTestPerInstruction(), List.of(generators));
  }

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(int runs,
                                                   Function<Integer, IssTestUtils.TestCase>... generators)
      throws IOException {
    return runTestsWith(runs, List.of(generators));
  }

  @SafeVarargs
  protected final List<IssTestUtils.TestCase> buildTestsWith(int runs,
                                                             Function<Integer, IssTestUtils.TestCase>... generators) {
    return buildTestsWith(runs, List.of(generators));
  }

  protected final Stream<DynamicTest> runTestsWith(
      List<Function<Integer, IssTestUtils.TestCase>> generators)
      throws IOException {
    return runTestsWith(getTestPerInstruction(), generators);
  }

  protected final List<IssTestUtils.TestCase> buildTestsWith(
      List<Function<Integer, IssTestUtils.TestCase>> generators) {
    return buildTestsWith(getTestPerInstruction(), generators);
  }

  protected final Stream<DynamicTest> runTestsWith(
      int runs,
      List<Function<Integer, IssTestUtils.TestCase>> generators) throws IOException {
    var image = generateIssSimulator(getVadlSpec());
    var testCases = buildTestsWith(runs, generators);
    return runQemuInstrTests(image, testCases);
  }

  /**
   * Materializes testcase specifications by invoking each generator {@code runs} times.
   *
   * <p>This method is pure with respect to ISS execution: it only creates
   * {@link IssTestUtils.TestCase} objects and does not run QEMU or containers.</p>
   */
  protected final List<IssTestUtils.TestCase> buildTestsWith(
      int runs,
      List<Function<Integer, IssTestUtils.TestCase>> generators) {
    if (generators.isEmpty()) {
      throw new IllegalArgumentException("No generators specified");
    }
    return generators.stream()
        .flatMap(genFunc -> IntStream.range(0, runs)
            .mapToObj(genFunc::apply)
        )
        .toList();
  }

  /**
   * Runs a QEMU instr test on the given image with the given test cases.
   *
   * @param image     the QEMU image to run the tests on
   * @param testCases the test cases passed to the container which runs the tests
   * @return the test result as DynamicTests as integration with JUnit
   */
  protected Stream<DynamicTest> runQemuInstrTests(DockerImage image,
                                                  Collection<IssTestUtils.TestCase> testCases)
      throws IOException {
    var failures = runQemuInstrTestsAndCollectFailures(image, testCases);
    return buildDynamicTests(testCases, failures);
  }

  protected Stream<DynamicTest> buildDynamicTests(
      Collection<IssTestUtils.TestCase> testCases,
      Map<String, String> failures) {
    return testCases.stream()
        .sorted(Comparator.comparing(IssTestUtils.TestCase::id))
        .map(testCase -> DynamicTest.dynamicTest(testCase.id(), () -> {
          var failure = failures.get(testCase.id());
          if (failure != null) {
            Assertions.fail(failure);
          }
        }));
  }

  /**
   * Executes the given testcase collection once and returns only the failing results.
   *
   * <p>The returned map is keyed by testcase id. Passing tests are intentionally omitted so
   * subclasses can cheaply render either flat dynamic tests or grouped containers from the cached
   * failure set.</p>
   */
  protected Map<String, String> runQemuInstrTestsAndCollectFailures(
      DockerImage image,
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
    var configuredJobs = System.getProperty("vadl.iss.jobs");
    // run the container and copy the test cases into the container
    // and after execution, copy the results from the container
    runContainer(image, container -> container
            .withCreateContainerCmdModifier(
                createContainerCmd -> createContainerCmd.withTty(true).withStdinOpen(true))
            .withCopyToContainer(MountableFile.forHostPath(testSuiteYaml.getPath()),
                "/work/test-suite.yaml")
            .withEnv(configuredJobs == null ? Map.of() : Map.of("VADL_ISS_JOBS", configuredJobs)),
        container -> copyPathFromContainer(container, "/work/results/", resultDirectory)
    );

    var expectedTests = testCases.stream()
        .collect(Collectors.toMap(
            IssTestUtils.TestCase::id,
            Function.identity(),
            (left, right) -> left,
            LinkedHashMap::new));
    Map<String, String> failures = new LinkedHashMap<>();

    try (var walkStream = java.nio.file.Files.walk(resultDirectory)) {
      walkStream
          .filter(file -> file.toString().endsWith(".yaml"))
          .forEach(file -> {
            var resultFile = file.toFile();
            var header = yamlToTestResultHeader(resultFile);
            var testCase = expectedTests.remove(header.id());
            if (testCase == null) {
              return;
            }
            if (header.status() == IssTestUtils.TestResult.Status.PASS) {
              return;
            }
            var result = yamlToTestResult(resultFile);
            failures.put(testCase.id(), buildFailureMessage(testCase, result));
          });
    } catch (Exception e) {
      Assertions.fail("Failed to load test results.", e);
    }

    return testCases.stream()
        .sorted(Comparator.comparing(IssTestUtils.TestCase::id))
        .<Map.Entry<String, String>>mapMulti((testCase, consumer) -> {
          var failure = validateResult(testCase, failures, expectedTests);
          if (failure != null) {
            consumer.accept(Map.entry(testCase.id(), failure));
          }
        })
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (left, right) -> left,
            LinkedHashMap::new));
  }

  private String validateResult(IssTestUtils.TestCase testCase,
                                Map<String, String> failures,
                                Map<String, IssTestUtils.TestCase> missingTests) {
    var failure = failures.get(testCase.id());
    if (failure != null) {
      return failure;
    }
    if (missingTests.containsKey(testCase.id())) {
      return "No result found for test " + testCase.id();
    }
    return null;
  }

  private String buildFailureMessage(IssTestUtils.TestCase testCase,
                                     IssTestUtils.TestResult result) {
    var sb = new StringBuilder();
    appendFailureDetails(sb, testCase, result);
    return sb.toString();
  }

  private void appendFailureDetails(StringBuilder sb,
                                    IssTestUtils.TestCase testSpec,
                                    IssTestUtils.TestResult result) {
    sb.append("Test failed for test: ").append(result.id()).append("\n");
    sb.append("ASM:\n").append(testSpec.asmCore()).append("\n");
    sb.append("Ran stages: ").append(result.completedStages()).append("\n");
    sb.append("Duration: ").append(result.duration()).append("\n");
    if (!result.errors().isEmpty()) {
      sb.append("Errors:\n");
      for (var error : result.errors()) {
        sb.append("- ").append(error).append("\n");
      }
    }
    if (!result.regTests().isEmpty()) {
      sb.append("Register diffs:\n");
      result.regTests().stream()
          .sorted(Comparator.comparing(IssTestUtils.TestResult.RegTestResult::reg))
          .forEachOrdered(r -> sb.append("- ")
              .append(r.reg())
              .append(" exp: ")
              .append(r.expected())
              .append(" act: ")
              .append(r.actual())
              .append("\n"));
    }
    if (!result.simLogs().isEmpty()) {
      sb.append("[SIM] Logs:\n");
      for (var entry : result.simLogs().entrySet()) {
        sb.append("- ").append(entry.getKey()).append(":\n");
        for (var line : entry.getValue()) {
          sb.append("  ").append(line).append("\n");
        }
      }
    }
    if (!result.refLogs().isEmpty()) {
      sb.append("[REF] Logs:\n");
      for (var entry : result.refLogs().entrySet()) {
        sb.append("- ").append(entry.getKey()).append(":\n");
        for (var line : entry.getValue()) {
          sb.append("  ").append(line).append("\n");
        }
      }
    }
  }

}
