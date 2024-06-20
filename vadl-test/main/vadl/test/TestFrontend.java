package vadl.test;

import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import vadl.viam.Specification;

/**
 * This interface allows VIAM tests to access an unknown frontend.
 * This allows to define tests in open-vadl while executing them from the old vadl project.
 *
 * <p>The old vadl implements the interface and sets the
 * {@link TestFrontend.Provider#globalFrontend} before executing the tests in the
 * {@link vadl.test} package.</p>
 */
public interface TestFrontend {

  Optional<Specification> runSpecification(Path vadlFile);

  /**
   * Holds the global frontend that can be dynamically set by the test executor.
   */
  class Provider {

    /**
     * The global frontend.
     *
     * <p>In order to run the tests defined here, the {@code globalFrontend} must be set
     * before running the tests. This is currently done in the old vadl project.</p>
     */
    @Nullable
    public static TestFrontend globalFrontend;
  }

}
