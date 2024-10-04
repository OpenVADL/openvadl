package vadl.pass;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.exception.PassError;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.ViamGraphError;

/**
 * A pass is a unit of execution. It analysis or transforms a VADL specification.
 */
public abstract class Pass {
  private GeneralConfiguration configuration;

  protected Pass(GeneralConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Get the name of the pass.
   */
  public abstract PassName getName();

  /**
   * Execute the pass on the {@link Specification}.
   *
   * @param passResults are the results from the different passes which have been executed so far.
   * @param viam        is latest VADL specification. Note that transformation passes are allowed
   *                    to mutate the object.
   * @return the result of the pass. This will be automatically stored into {@code passResults} for
   *     the next pass by the {@link PassManager}.
   */
  @Nullable
  public abstract Object execute(final PassResults passResults, Specification viam)
      throws IOException;

  /**
   * This method is a hook which gets invoked after the {@link #execute(PassResults, Specification)}
   * has run. It can be used to verify that all required exists.
   *
   * @param viam       is latest VADL specification. Note that transformation passes are allowed
   *                   to mutate the object.
   * @param passResult is the result of this pass class and can be {@code null}.
   */
  public void verification(Specification viam, @Nullable Object passResult) {

  }

  public GeneralConfiguration configuration() {
    return configuration;
  }
}
