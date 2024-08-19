package vadl.test.iss;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.shaded.com.google.common.collect.Streams;

public class Rv64iTest extends QemuExecutionTest {

  @TestFactory
  Stream<DynamicTest> test() throws InterruptedException, IOException {
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var image = getQemuTestImage(Path.of("./"), spec);

    var testSuiteYaml = testDirectory.resolve("test-suite.yaml").toFile();

    var testCases = IntStream.range(0, 1).mapToObj(i ->
        new TestSpec("ADD_" + i, Map.of("a0", "abcdef9876543210"),
            """
                li a0, 0xABCDEF9876543210
                li a1, 0x10000000
                li a2, 0xFFFFFF0001000FFF
                """)
    ).collect(Collectors.toMap(TestSpec::id, e -> e));
    writeTestSuiteConfigYaml(testCases.values(), testSuiteYaml);
    runContainerWithHostFsBind(image, testDirectory, "/work");

    var resultDir = testDirectory.resolve("results");
    var yamlResults = resultDir.toFile().listFiles((dir, name) -> name.endsWith(".yaml"));

    var testResults = new ArrayList<TestResult>();
    var failedToParse = new HashMap<String, Exception>();
    for (var file : yamlResults) {
      try {
        var result = yamlToTestResult(file);
        testResults.add(result);
      } catch (Exception e) {
        failedToParse.put(file.getName(), e);
      }
    }

    var specIds = testResults.stream()
        .map(TestResult::id)
        .collect(Collectors.toSet());

    var normalTestResultDynamicTests = testResults.stream()
        .map(e -> DynamicTest.dynamicTest(e.id(),
            () -> {
              var testSpec = testCases.get(e.id());
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

    var failedToParseDynamicTests = failedToParse.entrySet().stream()
        .map(e -> DynamicTest.dynamicTest("Parse " + e.getKey(),
            () -> {
              Assertions.fail("Failed to parse " + e.getKey(), e.getValue());
            }));

    var notFoundResultDynamicTests = testCases.values().stream()
        .filter(c -> !specIds.contains(c.id()))
        .filter(c -> !failedToParse.containsKey("result-" + c.id() + ".yaml"))
        .map(c -> DynamicTest.dynamicTest("Find result of " + c.id(),
            () -> Assertions.fail("No result found for test " + c.id())
        ));

    return Streams.concat(
        normalTestResultDynamicTests,
        failedToParseDynamicTests,
        notFoundResultDynamicTests
    );
  }

}
