package vadl.test.iss;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.MountableFile;
import org.yaml.snakeyaml.Yaml;
import vadl.test.DockerExecutionTest;
import vadl.viam.Identifier;
import vadl.viam.Specification;

/**
 * A QEMU execution test. It provides methods to handle the integration
 * with the QEMU docker test environment.
 */
public abstract class QemuExecutionTest extends DockerExecutionTest {

  private static final Logger logger = LoggerFactory.getLogger(QemuExecutionTest.class);

  // a cache that contains for the docker image with a newly created qemu build
  // for a Specification stored as its Identifier
  private static final Map<Identifier, ImageFromDockerfile> specQemuBuildImageCache = new HashMap<>();

  protected static synchronized ImageFromDockerfile getQemuTestImage(Path qemuSourceDir,
                                                                     Specification spec) {
    if (specQemuBuildImageCache.containsKey(spec.identifier)) {
      return specQemuBuildImageCache.get(spec.identifier);
    }

    var image = new ImageFromDockerfile()
        .withFileFromClasspath("images/llvm_riscv/Dockerfile", "/images/iss_qemu/Dockerfile")
        .withFileFromClasspath("/scripts/iss_qemu", "/scripts/iss_qemu");

    specQemuBuildImageCache.put(spec.identifier, image);
    return image;
  }

  @LazyInit
  public Path testDirectory;


  // The root directory for all qemu tests in the build directory.
  // We use the build directory as normal temporary directories are not mountable under linux
  // by default.
  private static Path buildTmpTestDirRoot = Path
          .of("build/tmp/open-vadl-qemu-test")
          .toAbsolutePath();
  private static AtomicInteger testDirId = new AtomicInteger(0);

  @BeforeAll
  public static void clearBuildTmpTestDir() throws IOException {
    var tmpDirFile = buildTmpTestDirRoot.toFile();
    if (tmpDirFile.exists()) {
        logger.debug("Removing old qemu test directory: {}", tmpDirFile);
      FileUtils.deleteDirectory(buildTmpTestDirRoot.toFile());
    }
    logger.debug("Create qemu root test directory: {}", tmpDirFile);
    if (!tmpDirFile.mkdirs()) {
      throw new IllegalStateException("Failed to create directory temporary qemu test dir" + tmpDirFile);
    }
  }

  @BeforeEach
  @Override
  public void beforeEach() {
    super.beforeEach();
    var testSpecificDirName = this.getClass().getSimpleName() + "-" + testDirId.incrementAndGet();
    testDirectory = buildTmpTestDirRoot.resolve(testSpecificDirName);

    logger.debug("Create test specific test directory: {}", testDirectory);
    if (!testDirectory.toFile().mkdirs()) {
      throw new IllegalStateException("Failed to create test directory " + testDirectory);
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
    var resultsYamlPath = testDirectory.resolve("results.yaml").toAbsolutePath();
    // write the test cases to this yaml file
    writeTestSuiteConfigYaml(testCases, testSuiteYaml);
    // run the container and copy the test cases into the container
    // and after execution, copy the results from the container
    runContainer(image, container -> container
            .withCopyToContainer(MountableFile.forHostPath(testSuiteYaml.getPath()), "/work/test-suite.yaml"),
        container -> container
            .copyFileFromContainer("/work/results.yaml",  resultsYamlPath.toString())
    );


    List<TestResult> testResults = List.of();

    try {
      // parse the results yaml file into a list of TestResults
      testResults = yamlToTestResults(resultsYamlPath.toFile());
    } catch (Exception e) {
      Assertions.fail("Failed to load test results.", e);
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
  protected static List<TestResult> yamlToTestResults(File yamlFile) throws IOException {
    try (var reader = new FileInputStream(yamlFile)) {
      Yaml yaml = new Yaml();
      // load all results
      List<Object> results = yaml.load(reader);

      return results.stream()
          // map yaml to test result
          .map(r -> {
            Map<String, Object> data = (Map<String, Object>) r;
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
          })
          .toList();
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
