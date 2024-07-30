package vadl.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import vadl.viam.Specification;

/**
 * The super type of all integration tests.
 * It sets up the vadl test frontend to run a vadl specification.
 */
public class AbstractTest {

  /**
   * The test source directory in the resources.
   * It contains all vadl test files.
   */
  public static final String TEST_SOURCE_DIR = "testSource";
  private static TestFrontend.Provider frontendProvider;
  private TestFrontend testFrontend;

  /**
   * Checks if the test frontend provider is available.
   */
  @BeforeAll
  public static void beforeAll() {
    var globalFrontendProvider = TestFrontend.Provider.globalProvider;
    if (globalFrontendProvider == null) {
      fail("Global frontend provider not set");
      Objects.requireNonNull(globalFrontendProvider);
    }
    frontendProvider = globalFrontendProvider;
  }

  /**
   * Retrieves the URI from the given Vadl source code.
   *
   * @param vadlSourceCode the Vadl source code
   * @return the URI of the temporary source file created from the source code
   * @throws RuntimeException if an IO exception occurs
   */
  public static URI getUriFromSourceCode(String vadlSourceCode) {
    File tempSourceFile = null;
    try {
      tempSourceFile = File.createTempFile("OpenVADL-", "-TestSource.vadl");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    tempSourceFile.deleteOnExit();

    return tempSourceFile.toURI();
  }

  /**
   * Loads the test source file from the resources and returns a URI.
   */
  public static URI getUriFromTestSource(String path) {
    var name =
        path.startsWith("/" + TEST_SOURCE_DIR + "/") ? path : "/" + TEST_SOURCE_DIR + "/" + path;

    // open resource as stream
    try (var resourceStream = frontendProvider.getClass().getResourceAsStream(name)) {
      assertNotNull(resourceStream, "Resource not found: " + name);

      // create temporary file for test source
      var tempFile =
          File.createTempFile("OpenVADL-", "-"
              + path.substring(path.lastIndexOf("/") + 1));
      tempFile.deleteOnExit();

      // copy resource stream into temporary file
      // this is required as the source file is in the resource directory within
      // a jar, and therefore vadl cannot directly access it via a path.
      Files.copy(resourceStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

      return tempFile.toURI();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    var sourceUri = getUriFromTestSource(testSourcePath);
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
    var sourceUri = getUriFromTestSource(testSourcePath);
    var success = testFrontend.runSpecification(sourceUri);
    if (!success) {
      var logs = testFrontend.getLogAsString();
      var errorLogs = logs.substring(logs.indexOf(" error: "));
      fail(errorLogs);
    }
    return testFrontend.getViam();
  }
}
