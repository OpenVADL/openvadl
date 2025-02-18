package vadl;

import java.net.URI;
import javax.annotation.Nullable;
import vadl.viam.Specification;

/**
 * This interface allows VIAM tests to access an unknown frontend.
 * This allows to define tests in open-vadl while executing them from the old vadl project.
 *
 * <p>The old vadl implements the interface and sets the
 * {@link Provider#globalProvider} before executing the tests in the
 * {@link vadl.test} package.</p>
 */
public interface TestFrontend {

  /**
   * Runs the specification until AST to VIAM conversion is done.
   *
   * @param vadlFile the specification file
   * @return true if success, otherwise false
   */
  boolean runSpecification(URI vadlFile);

  /**
   * Get the VIAM from the run result. This must be called after
   * {@link TestFrontend#runSpecification}.
   */
  Specification getViam();

  /**
   * Get the logs that were emitted during execution as String.
   */
  String getLogAsString();

  /**
   * Holds the global frontend provider that can be dynamically set by the test executor.
   */
  abstract class Provider {

    /**
     * The global frontend provider.
     *
     * <p>In order to run the tests defined here, the {@code globalProvider} must be set
     * before running the tests. This is currently done in the old vadl project.</p>
     */
    @Nullable
    public static Provider globalProvider = new OpenVadlTestFrontend.Provider();

    /**
     * Creates a new instance of the {@link TestFrontend}.
     */
    public abstract TestFrontend createFrontend();
  }
}
