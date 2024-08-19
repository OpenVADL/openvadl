package vadl.test.iss;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import vadl.test.DockerExecutionTest;
import vadl.utils.Pair;
import vadl.utils.VADLFileUtils;
import vadl.viam.Identifier;
import vadl.viam.Specification;

/**
 * A QEMU execution test. It provides methods to handle the integration
 * with the QEMU docker test environment.
 */
public abstract class QemuExecutionTest extends DockerExecutionTest {

  // a cache that contains for the docker image with a newly created qemu build
  // for a Specification stored as its Identifier
  private static Map<Identifier, ImageFromDockerfile> specQemuBuildImageCache = new HashMap<>();

  protected static synchronized ImageFromDockerfile getQemuTestImage(Path qemuSourceDir,
                                                                     Specification spec) {
    if (specQemuBuildImageCache.containsKey(spec.identifier)) {
      return specQemuBuildImageCache.get(spec.identifier);
    }

    var image = new ImageFromDockerfile()
        .withFileFromClasspath("Dockerfile", "/images/iss_qemu/Dockerfile")
        .withFileFromClasspath("/scripts/iss_qemu", "/scripts/iss_qemu");

    specQemuBuildImageCache.put(spec.identifier, image);
    return image;
  }

  @LazyInit
  public Path testDirectory;

  @BeforeEach
  @Override
  public void beforeEach() {
    super.beforeEach();
    try {
      testDirectory =
          VADLFileUtils.createTempDirectory(
              "QEMU-Exec-Test-" + this.getClass().getSimpleName() + "-");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Runs a QEMU instr test on the given image with the given test cases.
   *
   * @param image     the QEMU image to run the tests on
   * @param testCases the test cases passed to the container which runs the tests
   * @return the test result as DynamicTests as integration with JUnit
   */
  protected Stream<DynamicTest> runQemuInstrTests(ImageFromDockerfile image,
                                                  Collection<TestSpec> testCases)
      throws IOException {
    // resolve file that contains all test specifications.
    // it is a yaml file that gets mapped to `/work/test-suite.yaml` of the container.
    var testSuiteYaml = testDirectory.resolve("test-suite.yaml").toFile();
    // write the test cases to this yaml file
    writeTestSuiteConfigYaml(testCases, testSuiteYaml);
    // run the container with a binding of the temporary test directory to "/work"
    runContainerWithHostFsBind(image, testDirectory, "/work");

    // the container will places the test results under /work/results
    var resultDir = testDirectory.resolve("results");
    // the result of each spec is in a separate yaml file under /work/results
    // so we want to get all those files
    var yamlResults = resultDir.toFile()
        .listFiles((dir, name) -> name.endsWith(".yaml"));

    var testResults = new ArrayList<TestResult>();
    // all files that could not be parsed (should not happen)
    var failedToParse = new HashMap<String, Pair<File, Exception>>();

    assert yamlResults != null;
    // parse all the results in classes
    for (var file : yamlResults) {
      try {
        var result = yamlToTestResult(file);
        testResults.add(result);
      } catch (Exception e) {
        failedToParse.put(file.getName(), Pair.of(file, e));
      }
    }

    // just for fast access later
    var specIds = testResults.stream()
        .map(TestResult::id)
        .collect(Collectors.toSet());
    var testCaseMap = testCases.stream()
        .collect(Collectors.toMap(TestSpec::id, s -> s));

    // produce DynamicTests for all parsed test results.
    // these will be listed in the JUnit test report
    var normalTestResultDynamicTests = testResults.stream()
        .map(e -> DynamicTest.dynamicTest(e.id(),
            () -> {
              var testSpec = testCaseMap.get(e.id());
              System.out.println("Test " + e.id());
              System.out.println("ASM: \n" + testSpec.asmCore());
              System.out.println("-------");
              System.out.println("Ran stages: " + e.completedStages());
              System.out.println("Register tests: \n" + e.regTests());
              System.out.println("Duration: " + e.duration());

              Assertions.assertEquals(TestResult.Status.PASS, e.status(),
                  String.join(", ", e.errors()));
            }
        ));

    // produce tests for the results that could not be parsed
    var failedToParseDynamicTests = failedToParse.entrySet().stream()
        .map(e -> DynamicTest.dynamicTest("Parse " + e.getKey(),
            () -> {
              System.out.println("Content of " + e.getKey() + ": \n");
              try (var lineStream = Files.lines(e.getValue().left().toPath())) {
                lineStream.forEach(System.out::println);
              }
              Assertions.fail("Failed to parse " + e.getKey(), e.getValue().right());
            }));

    // produces dynamic tests for all cases where no result was found
    var notFoundResultDynamicTests = testCases.stream()
        .filter(c -> !specIds.contains(c.id()))
        .filter(c -> !failedToParse.containsKey("result-" + c.id() + ".yaml"))
        .map(c -> DynamicTest.dynamicTest("Find result of " + c.id(),
            () -> Assertions.fail("No result found for test " + c.id())
        ));

    // return stream of all dynamic test cases
    return Streams.concat(
        normalTestResultDynamicTests,
        failedToParseDynamicTests,
        notFoundResultDynamicTests
    );
  }

  /**
   * Writes the test suite configuration YAML file.
   *
   * @param specs The collection of test specifications.
   * @param dest  The destination file to write the YAML configuration.
   * @throws IOException if an I/O error occurs.
   */
  protected static void writeTestSuiteConfigYaml(Collection<TestSpec> specs, File dest)
      throws IOException {
    var specsYaml = specs.stream().map(spec -> {
      var specYaml = new LinkedHashMap<String, Object>();
      specYaml.put("id", spec.id);
      specYaml.put("reg_tests", spec.regTests);
      specYaml.put("asm_core", spec.asmCore);
      return specYaml;
    }).toList();

    var yamlSuite = new LinkedHashMap<>();
    yamlSuite.put("tests", specsYaml);
    Yaml yaml = new Yaml();
    try (var writer = new FileWriter(dest)) {
      yaml.dump(yamlSuite, writer);
    }
  }

  /**
   * Converts a YAML file to a TestResult object.
   *
   * @param yamlFile The YAML file to convert.
   * @return The converted TestResult object.
   * @throws IOException if an I/O error occurs.
   */
  protected static TestResult yamlToTestResult(File yamlFile) throws IOException {
    try (var reader = new FileInputStream(yamlFile)) {
      Yaml yaml = new Yaml();
      Map<String, Object> data = yaml.load(reader);
      Map<String, Object> result = (Map<String, Object>) data.get("result");

      String id = data.get("id").toString();
      // Assuming the YAML structure matches the fields in TestResult
      TestResult.Status status = TestResult.Status.valueOf((String) result.get("status"));
      List<TestResult.Stage> completedStages =
          ((List<String>) result.get("completedStages")).stream()
              .map(e -> TestResult.Stage.valueOf(e))
              .toList();
      List<String> errors = (List<String>) result.get("errors");
      String duration = (String) result.get("duration");
      List<TestResult.RegTestResult> regTests =
          ((Map<String, Object>) result.get("regTests")).entrySet()
              .stream().map(e -> {
                var val = (Map<String, String>) e.getValue();
                return new TestResult.RegTestResult(e.getKey(), val.get("expected"), val.get("actual"));
              }).toList();

      return new TestResult(id, status, completedStages, regTests, errors, duration);
    }
  }

  protected record TestSpec(
      String id,
      Map<String, String> regTests,
      String asmCore
  ) {
  }

  protected record TestResult(
      String id,
      Status status,
      List<Stage> completedStages,
      List<RegTestResult> regTests,
      List<String> errors,
      String duration
  ) {

    protected enum Status {
      PASS,
      FAIL
    }

    protected enum Stage {
      COMPILE,
      LINK,
      RUN
    }

    protected record RegTestResult(
        String reg,
        String expected,
        String actual
    ) {
    }
  }


}
