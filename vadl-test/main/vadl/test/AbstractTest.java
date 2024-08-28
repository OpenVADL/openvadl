package vadl.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import javax.annotation.Nullable;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassKey;
import vadl.pass.PassManager;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.VADLFileUtils;
import vadl.viam.Specification;
import vadl.viam.passes.verification.ViamVerificationPass;
import vadl.viam.passes.verification.ViamVerifier;

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

  private TestFrontend testFrontend;

  /**
   * Checks if the test frontend provider is available.
   */
  @BeforeAll
  public static synchronized void beforeAll() throws URISyntaxException, IOException {
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
      testSourceRootPath = VADLFileUtils.copyDirToTempDir(
          Objects.requireNonNull(testSourceDir).toURI(),
          "OpenVADL-testSource-",
          (pair) -> {
            var reader = pair.left();
            var writer = pair.right();
            // call velocity to evaluate the potential template and write the result
            // into the outFileWriter.
            // we use an empty context
            Velocity.evaluate(new VelocityContext(), writer, "OpenVADL", reader);
          }
      );
    }
  }

  /**
   * Resolves the test source's path to an absolute URI.
   */
  public static URI getTestSourcePath(String path) {
    var prefixToRemove = "/" + TEST_SOURCE_DIR + "/";
    var subPath = path.startsWith(prefixToRemove)
        ? path.substring(prefixToRemove.length())
        : path;

    return testSourceRootPath.resolve(subPath).toUri();
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

    assertThat(testSources,
        containsInAnyOrder(
            preparedArgs.stream().map(e -> e.get()[0]).toArray()
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
      throws URISyntaxException {
    var fileNames = new ArrayList<String>();
    File directory = new File(testSourceUrl.toURI());
    File[] files = directory.listFiles((dir, name) -> name.startsWith(prefix));
    if (files != null) {
      for (File file : files) {
        fileNames.add(file.getPath());
      }
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
  public void beforeEach() {
    testFrontend = frontendProvider.createFrontend();
  }

  public TestFrontend testFrontend() {
    return testFrontend;
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
    var success = testFrontend.runSpecification(sourceUri);
    if (success) {
      fail("Assumed failure for specification " + testSourcePath + " but succeeded");
    }
    if (failureMessage != null) {
      var logs = testFrontend.getLogAsString();
      var errorLogs = logs.substring(logs.indexOf(" error: "));
      assertThat(errorLogs, containsString(failureMessage));
    }
  }

  /**
   * Runs the specification and returns the VIAM representation.
   *
   * @param testSourcePath the path of the test source file
   * @return the VIAM specification
   */
  public Specification runAndGetViamSpecification(String testSourcePath) {
    tryToRunSpecificationWithFrontend(testSourcePath, testFrontend);
    return testFrontend.getViam();
  }

  public static TestFrontend runViamSpecificationWithNewFrontend(String testSourcePath) {
    var newFrontend = frontendProvider.createFrontend();
    tryToRunSpecificationWithFrontend(testSourcePath, newFrontend);
    return newFrontend;
  }

  /**
   * Tries to run the given test source path (not the resolved one) with the given frontend.
   * It will fail if the run was not successful.
   */
  private static void tryToRunSpecificationWithFrontend(String testSourcePath,
                                                        TestFrontend frontend) {
    var testSource = getTestSourcePath(testSourcePath);
    var success = frontend.runSpecification(testSource);
    if (!success) {
      var logs = frontend.getLogAsString();
      var errorLogs = logs.substring(logs.indexOf(" error: "));

      System.out.println(
          "Test source: ---------------\n" + testSourceToString(testSource) + "\n---------------");
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

  public static Path createDirectory() throws IOException {
    return VADLFileUtils.createTempDirectory("vadl-test");
  }

  public GeneralConfiguration getConfiguration(boolean doDump) throws IOException {
    var directory = createDirectory();
    return new GeneralConfiguration(directory.toAbsolutePath().toString(), doDump);
  }

  public record TestSetup(PassManager passManager,
                          Specification specification) {

  }
}
