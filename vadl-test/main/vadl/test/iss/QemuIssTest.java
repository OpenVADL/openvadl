package vadl.test.iss;

import static vadl.test.iss.IssTestUtils.writeTestSuiteConfigYaml;
import static vadl.test.iss.IssTestUtils.yamlToTestResults;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import org.testcontainers.utility.MountableFile;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.DockerExecutionTest;

/**
 * The test class to build and run tests on the QEMU ISS.
 * The {@link #generateSimulator(String)} methods runs the ISS generation and builds
 * a working QEMU image with the new target.
 * Every target specification is cached and therefore only built for the first test.
 *
 * <p>The class also provides functions to automatically run tests in the container.</p>
 */
public abstract class QemuIssTest extends DockerExecutionTest {

  // config of qemu test image
  private static final String QEMU_TEST_IMAGE =
      "jozott/qemu@sha256:59fd89489908864d9c5267a39152a55559903040299bcb7a65382c4beebac2e2";

  // specification to image cache
  private static final ConcurrentHashMap<String, ImageFromDockerfile> issImageCache =
      new ConcurrentHashMap<>();

  private static final Logger log = LoggerFactory.getLogger(QemuIssTest.class);

  /**
   * This will run the given specification and produces a working docker image that contains
   * a compiled QEMU ISS from the specification.
   *
   * <p>If the specification was already build by some other test, the image is reused.</p>
   *
   * @param specPath path to VADL specification in testSource
   * @return the image containing the generated QEMU ISS
   */
  protected ImageFromDockerfile generateSimulator(String specPath) {
    return issImageCache.computeIfAbsent(specPath, (path) -> {
      try {
        var config = IssConfiguration.from(getConfiguration(false));
        // run iss generation
        setupPassManagerAndRunSpec(path, PassOrders.iss(config));

        // find iss output path
        var issOutputPath = Path.of(config.outputPath() + "/iss").toAbsolutePath();
        if (!issOutputPath.toFile().exists()) {
          throw new IllegalStateException("ISS output path was not found (not generated?)");
        }

        // generate iss image from the output path
        return getIssImage(issOutputPath);
      } catch (IOException | DuplicatedPassKeyException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Runs a QEMU instr test on the given image with the given test cases.
   *
   * @param image     the QEMU image to run the tests on
   * @param testCases the test cases passed to the container which runs the tests
   * @return the test result as DynamicTests as integration with JUnit
   */
  protected Stream<DynamicTest> runQemuInstrTests(ImageFromDockerfile image,
                                                  Collection<IssTestUtils.TestSpec> testCases)
      throws IOException {
    // resolve file that contains all test specifications.
    // it is a yaml file that gets mapped to `/work/test-suite.yaml` of the container.
    var testSuiteYaml = getTestDirectory().resolve("test-suite.yaml").toFile();
    var resultsYamlPath = getTestDirectory().resolve("results.yaml").toAbsolutePath();
    // write the test cases to this yaml file
    writeTestSuiteConfigYaml(testCases, testSuiteYaml);
    // run the container and copy the test cases into the container
    // and after execution, copy the results from the container
    runContainer(image, container -> container
            .withCopyToContainer(MountableFile.forHostPath(testSuiteYaml.getPath()),
                "/work/test-suite.yaml"),
        container -> container
            .copyFileFromContainer("/work/results.yaml", resultsYamlPath.toString())
    );


    List<IssTestUtils.TestResult> testResults = List.of();

    try {
      // parse the results yaml file into a list of TestResults
      testResults = yamlToTestResults(resultsYamlPath.toFile());
    } catch (Exception e) {
      Assertions.fail("Failed to load test results.", e);
    }

    // just for fast access later
    var specIds = testResults.stream()
        .map(IssTestUtils.TestResult::id)
        .collect(Collectors.toSet());
    var testCaseMap = testCases.stream()
        .collect(Collectors.toMap(IssTestUtils.TestSpec::id, s -> s));

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
                for (var log : e.logs().entrySet()) {
                  System.out.println("Logs of " + log.getKey() + ": ");
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


  /**
   * This will produce a new image for the given generated iss sources.
   *
   * @param generatedIssSources the path to the generated ISS/QEMU sources.
   * @return a new image that builds the ISS at build time.
   */
  private ImageFromDockerfile getIssImage(Path generatedIssSources
  ) {

    // get redis cache for faster compilation using sccache
    var redisCache = getRunningRedisCache();

    var dockerImage = new ImageFromDockerfile()
        .withDockerfileFromBuilder(d -> {
              d
                  .from(QEMU_TEST_IMAGE)
                  .run("apt install -y netcat")
                  // TODO: Remove when using updated docker image
                  .run("pip install pyyaml qemu.qmp")
                  .copy("iss", "/qemu")
                  // TODO: Move this to prebuilt docker image
                  .workDir("/qemu/build");
              d.run("mv qemu-system-riscv64 /bin/qemu-system-riscv64");
              // TODO remove this when we updated the docker image
              d.run("rm -rf *");
              d.run("qemu-system-riscv64 --version");

              // use redis cache for building (sccache allows remote caching)
              var cc = "sccache gcc";
              redisCache.setupEnv(d);

              // TODO: update target name
              // configure qemu with the new target from the specification
              d.run("../configure --cc='" + cc + "' --target-list=vadl-softmmu");
              // build qemu with all cpu cores and print if cache was used
              d.run("make -j$(nproc) && sccache -s");
              // validate existence of generated qemu iss
              d.run("qemu-system-vadl --version");

              d.workDir("/work");

              d.copy("/scripts", "/scripts");
              d.run("ls /scripts");
              d.cmd("python3 /scripts/bare_metal_runner.py /qemu/build/qemu-system-vadl");

              d.build();
            }
        )
        // make iss sources available to image builder
        .withFileFromPath("iss", generatedIssSources)
        // make iss_qemu scripts available to image builder
        .withFileFromClasspath("/scripts", "/scripts/iss_qemu");

    // as we have to use the same network as the redis cache, we have to build it there
    return redisCache.setupEnv(dockerImage);
  }

}
