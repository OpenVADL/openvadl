package vadl.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import vadl.viam.Specification;

/**
 * The super type of all integration tests.
 * It sets up the vadl test frontend to run a vadl specification.
 */
public class AbstractTest {

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
   * Runs the specification and returns the VIAM representation.
   *
   * @param testSourcePath the path of the test source file
   * @return the VIAM specification
   */
  public Specification runAndGetViamSpecification(String testSourcePath) {
    var sourceUri = getUriFromTestSource(testSourcePath);
    var success = testFrontend.runSpecification(sourceUri);
    if (!success) {
      fail(testFrontend.getLogAsString());
    }
    return testFrontend.getViam();
  }


  /**
   * Retrieves the URI from the given Vadl source code.
   *
   * @param vadlSourceCode the Vadl source code
   * @return the URI of the temporary source file created from the source code
   * @throws RuntimeException if an IO exception occurs
   */
  public URI getUriFromSourceCode(String vadlSourceCode) {
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
  public URI getUriFromTestSource(String path) {
    var name = path.startsWith("/testSource/") ? path : "/testSource/" + path;

    // open resource as stream
    try (var resourceStream = getClass().getResourceAsStream(name)) {
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


}
