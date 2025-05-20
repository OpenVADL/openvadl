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

package vadl;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import javax.annotation.Nullable;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassKey;
import vadl.pass.PassManager;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.VadlFileUtils;
import vadl.viam.Specification;

/**
 * The super type of all integration tests.
 * It sets up the vadl test frontend to run a vadl specification.
 */
public abstract class AbstractTest {

  /**
   * The test source directory in the resources.
   * It contains all vadl test files.
   */
  public static final String TEST_SOURCE_DIR = "testSource";
  private static TestFrontend.Provider frontendProvider;
  private static Path testSourceRootPath;

  // @TestFactory won't trigger beforeAll, so we must call it manually from `beforeEach`.
  private static boolean beforeAllRan = false;

  private TestFrontend testFrontend;

  // directory of the current test (used to emit files)
  @Nullable
  private Path testDirectory;

  /**
   * Checks if the test frontend provider is available.
   */
  @BeforeAll
  public static synchronized void beforeAll() throws URISyntaxException, IOException {
    if (beforeAllRan) {
      return;
    }
    beforeAllRan = true;

    // check if global provider is set
    var globalFrontendProvider = TestFrontend.Provider.globalProvider;
    if (globalFrontendProvider == null) {
      fail("Global frontend provider not set");
      Objects.requireNonNull(globalFrontendProvider);
    }
    frontendProvider = globalFrontendProvider;

    if (testSourceRootPath == null) {
      // load all testsources in a temporary directory
      var testSourceDir = AbstractTest.class.getResource("/" + TEST_SOURCE_DIR);
      testSourceRootPath = VadlFileUtils.copyDirToTempDir(
          Objects.requireNonNull(testSourceDir).toURI(),
          "OpenVADL-testSource-",
          null
      );
    }
  }

  /**
   * Resolves the test source's path to an absolute URI.
   */
  public static Path getTestSourcePath(String path) {
    var prefixToRemove = "/" + TEST_SOURCE_DIR + "/";
    var subPath = path.startsWith(prefixToRemove)
        ? path.substring(prefixToRemove.length())
        : path;

    return testSourceRootPath.resolve(subPath);
  }

  /**
   * Retrieves the test source arguments for a parameterized test.
   *
   * @param sourcePrefix the prefix of the test source files
   * @param args         the arguments for the parameterized test
   * @return a stream of arguments for the parameterized test
   */
  public static Stream<Arguments> getTestSourceArgsForParameterizedTest(
      String sourcePrefix,
      Arguments... args
  ) {
    var testSources = findAllTestSources(sourcePrefix);
    var preparedArgs = Stream.of(args)
        .map(e -> {
          assertEquals(2, e.get().length, "Wrong number of arguments for " + e);
          return arguments(sourcePrefix + e.get()[0] + ".vadl", e.get()[1]);
        })
        .toList();

    List<String> expectedSubstrings = preparedArgs.stream()
        .map(e -> (String) e.get()[0])
        .toList();

    assertThat("Some test source not found", testSources,
        hasItems(preparedArgs.stream()
            .map(e -> containsString((String) e.get()[0]))
            .toArray(Matcher[]::new)
        ));

    return preparedArgs.stream();
  }

  /**
   * Finds all test sources with the given prefix.
   *
   * @param prefix the prefix of the test source files
   * @return a list of test source file paths
   * @throws RuntimeException if an IO exception occurs
   */
  public static List<String> findAllTestSources(String prefix) {
    var resourceUrl =
        Objects.requireNonNull(
            // just get some class for resource fetching
            frontendProvider.getClass()
                .getResource("/" + TEST_SOURCE_DIR + "/"));

    List<String> fileNames;
    try {
      if (resourceUrl.getProtocol().equals("jar")) {
        fileNames = findAllTestSourcesFromJar(resourceUrl, prefix);
      } else if (resourceUrl.getProtocol().equals("file")) {
        fileNames = findAllTestSourcesFromFileSystem(resourceUrl, prefix);
      } else {
        throw new RuntimeException("Unsupported protocol: " + resourceUrl.getProtocol());
      }
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return fileNames.stream()
        .map(e -> e.startsWith(TEST_SOURCE_DIR)
            ? e.substring(TEST_SOURCE_DIR.length() + 1)
            : e)
        .toList();
  }

  private static List<String> findAllTestSourcesFromFileSystem(URL testSourceUrl, String prefix)
      throws IOException, URISyntaxException {
    List<String> fileNames = new ArrayList<>();
    Path startPath = Paths.get(testSourceUrl.toURI());

    try (Stream<Path> stream = Files.walk(startPath)) {
      var paths = stream.toList();
      paths.stream().filter(
              file -> Files.isRegularFile(file)
                  && startPath.relativize(file).toString().startsWith(prefix))
          .forEach(file -> fileNames.add(file.toString()));
    }

    return fileNames;
  }

  private static List<String> findAllTestSourcesFromJar(URL testSourceUrl, String prefix)
      throws IOException {
    var fileNames = new ArrayList<String>();
    var jarPath = testSourceUrl.getPath().substring(5, testSourceUrl.getPath().indexOf("!"));

    try (JarFile jar = new JarFile(jarPath)) {
      var entries = jar.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!entry.isDirectory() && entry.getName().startsWith(TEST_SOURCE_DIR + "/" + prefix)) {
          fileNames.add(entry.getName());
        }
      }
      return fileNames;
    }
  }

  /**
   * Creates a new test frontend for every test execution.
   */
  @BeforeEach
  public void beforeEach() throws URISyntaxException, IOException {
    if (!beforeAllRan) {
      beforeAll();
    }
    testFrontend = frontendProvider.createFrontend();
  }

  /**
   * Runs the given test source file and assumes that it will fail. If the test source file does
   * not fail or if the provided failure message is not found in the error logs, the method
   * will fail.
   *
   * @param testSourcePath the path of the test source file
   * @param failureMessage the message to search for in the error logs (optional)
   */
  public void runAndAssumeFailure(String testSourcePath, @Nullable String failureMessage) {
    var sourceUri = getTestSourcePath(testSourcePath);
    var success = testFrontend.runSpecification(sourceUri.toUri());
    if (success) {
      fail("Assumed failure for specification " + testSourcePath + " but succeeded");
    }
    if (failureMessage != null) {
      var logs = testFrontend.getLogAsString();
      var errorIndex = logs.indexOf(" error: ");
      var errorLogs = logs;
      if (errorIndex != -1) {
        errorLogs = errorLogs.substring(errorIndex);
      }
      assertThat(errorLogs, containsString(failureMessage));
    }
  }

  /**
   * Runs the specification and returns the VIAM representation.
   *
   * @param testSourcePath the resource {@code testSource} relative path of the test source file
   * @return the VIAM specification
   */
  public Specification runAndGetViamSpecification(String testSourcePath) {
    var sourcePath = getTestSourcePath(testSourcePath);
    return runAndGetViamSpecification(sourcePath);
  }

  /**
   * Runs the specification and returns the VIAM representation.
   *
   * @param sourcePath the absolute path to the test source file
   * @return the VIAM specification
   */
  public Specification runAndGetViamSpecification(Path sourcePath) {
    if (!sourcePath.isAbsolute()) {
      throw new IllegalArgumentException("Source path must be absolute");
    }
    tryToRunSpecificationWithFrontend(sourcePath, testFrontend);
    return testFrontend.getViam();
  }

  /**
   * Tries to run the given test source (the resolved one) with the given frontend.
   * It will fail if the run was not successful.
   *
   * @param sourcePath The concrete resolved source path of the specification
   */
  private static void tryToRunSpecificationWithFrontend(Path sourcePath,
                                                        TestFrontend frontend) {
    var success = frontend.runSpecification(sourcePath.toUri());
    if (!success) {
      var logs = frontend.getLogAsString();
      var errorIndex = logs.indexOf(" error: ");
      var errorLogs = logs;
      if (errorIndex != -1) {
        errorLogs = errorLogs.substring(errorIndex);
      }

      System.out.println(
          "Test source: ---------------\n" + testSourceToString(sourcePath.toUri())
              + "\n---------------");
      fail(errorLogs);
    }
  }

  /**
   * Returns the current test source code as a formatted string.
   * This must be called after calling {@link #runAndGetViamSpecification(String)}.
   * The output also includes line numbers.
   */
  public static String testSourceToString(URI sourceUri) {
    StringBuilder result = new StringBuilder();
    try {
      var sourceFile = new File(sourceUri);
      // Determine the total number of lines to calculate padding
      int totalLines = 0;
      try (BufferedReader lineCounter = new BufferedReader(
          new FileReader(sourceFile))) {
        while (lineCounter.readLine() != null) {
          totalLines++;
        }
      }

      // Calculate the number of digits in the last line number
      int maxDigits = String.valueOf(totalLines).length();

      // Read and format each line with padded line numbers
      try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
        String line;
        int lineNumber = 1;
        String numberFormat = "%-" + maxDigits + "d|  ";  // Create a left-padded number format
        while ((line = reader.readLine()) != null) {
          result.append(String.format(numberFormat, lineNumber)).append(line).append("\n");
          lineNumber++;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result.toString();
  }


  public TestSetup setupPassManagerAndRunSpec(String specPath,
                                              PassOrder passes)
      throws IOException, DuplicatedPassKeyException {
    var spec = runAndGetViamSpecification(specPath);

    var passManager = new PassManager();
    passManager.add(passes);
    passManager.run(spec);

    return new TestSetup(passManager, spec);
  }


  /**
   * Sets the PassManager and runs the provided specification with the pass order.
   *
   * @deprecated Use {@link #setupPassManagerAndRunSpec(String, PassOrder)} instead and use the
   *     {@link PassOrder#untilFirst(Class)} method instead.
   */
  @Deprecated
  public TestSetup setupPassManagerAndRunSpecUntil(String specPath,
                                                   PassOrder passes,
                                                   PassKey until)
      throws IOException, DuplicatedPassKeyException {
    var spec = runAndGetViamSpecification(specPath);

    var passManager = new PassManager();
    passManager.add(passes);
    passManager.runUntilInclusive(spec, until);

    return new TestSetup(passManager, spec);
  }

  /**
   * Returns the path to the current test directory.
   */
  public synchronized Path getTestDirectory() {
    if (testDirectory == null) {
      try {
        testDirectory = createDirectory();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return testDirectory;
  }

  public GeneralConfiguration getConfiguration(boolean doDump) {
    var directory = getTestDirectory();
    return new GeneralConfiguration(directory.toAbsolutePath(), doDump);
  }

  public record TestSetup(PassManager passManager,
                          Specification specification) {

  }

  /**
   * Vadl create a new temporary directory under the vadl-test namespace.
   */
  protected static Path createDirectory() throws IOException {
    return VadlFileUtils.createTempDirectory("vadl-test");
  }
}
