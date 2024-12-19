package vadl.test.iss;


import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.utility.MountableFile;

public class IssCustomTests extends QemuIssTest {


  @TestFactory
  Stream<DynamicTest> customTests() {
    var qemuImage = generateIssSimulator("sys/risc-v/rv64i.vadl");

    // Find test source directory and results directory
    var testSources = getTestSourcePath("iss/riscv/custom");
    var resultsDir = getTestDirectory().resolve("results");

    // Run the QEMU container to execute the tests and collect results
    runContainer(qemuImage, container ->
            container.withCopyFileToContainer(MountableFile.forHostPath(testSources + "/"),
                    "/work")
                .withCommand("python3 exec-all.py"),
        container ->
            copyPathFromContainer(container, "/work/results", resultsDir)
    );

    // List all test source files
    var testSourceFiles = Arrays.stream(testSources.resolve("tests").toFile().listFiles())
        .filter(file -> file.isFile() && file.getName().endsWith(".S"))
        .map(File::getName)
        .collect(Collectors.toSet());

    // List all result files
    var resultFiles = Arrays.stream(resultsDir.toFile().listFiles())
        .filter(file -> file.isFile() && file.getName().endsWith(".result"))
        .collect(Collectors.toMap(
            File::getName,
            file -> {
              try {
                return Files.readString(file.toPath());
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            }
        ));

    // Check if there are missing results
    var missingResults = testSourceFiles.stream()
        .filter(test -> !resultFiles.containsKey(test.replace(".S", ".result")))
        .collect(Collectors.toSet());

    if (!missingResults.isEmpty()) {
      System.err.println("Missing results for tests: " + missingResults);
    }

    // Generate dynamic tests
    return testSourceFiles.stream()
        .map(test -> DynamicTest.dynamicTest(test, () -> {
          var resultFileName = test.replace(".S", ".result");
          var resultOutput = resultFiles.get(resultFileName);

          // Fail if the result file is missing
          if (resultOutput == null) {
            fail("Result file is missing for test: " + test);
          }

          // Parse the result file
          if (!resultOutput.contains("SUCCESS")) {
            System.out.println("Test failed: " + test);
            System.out.println("Output:\n" + resultOutput);
            fail("Test failed: " + test);
          }
        }));
  }

}
