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

  /// RUNTIME CHECK HELPERS

  /**
   * Ensures that a given condition is true. If the condition is false, an exception is thrown
   * with the provided format string and arguments.
   *
   * <p>The thrown exception has context information about the node and graph.</p>
   *
   * @param condition the condition to check
   * @param format    the format string for the exception message
   * @param args      the arguments to replace in the format string
   * @throws PassError if the condition is false
   */
  @FormatMethod
  @Contract("false, _, _-> fail")
  protected final void ensure(boolean condition, @FormatString String format,
                              @Nullable Object... args) {
    if (!condition) {
      throw new PassError(format, args)
          .shrinkStacktrace(1);
    }
  }

  /**
   * Ensures that a given condition is true. If the condition is false, an exception is thrown
   * with the provided format string and arguments.
   *
   * <p>The thrown exception has context information about the node and graph.</p>
   *
   * @param condition          the condition to check
   * @param diagnosticSupplier is the function which provides the {@link Diagnostic}.
   * @throws Diagnostic if the condition is false
   */
  protected final void ensure(boolean condition, Supplier<Diagnostic> diagnosticSupplier) {
    if (!condition) {
      throw diagnosticSupplier.get();
    }
  }

  /**
   * Ensures that the given object is not null. If the object is null, an exception is thrown
   * with the specified message.
   *
   * <p>The thrown exception has context information about the node and graph.</p>
   *
   * @param obj the object to check for null
   * @param msg the message to include in the exception if the object is null
   * @throws PassError if the object is null
   */
  @Contract("null, _  -> fail")
  @FormatMethod
  protected final <T> T ensureNonNull(@Nullable T obj, String msg) {
    ensure(obj != null, msg);
    return obj;
  }

  /**
   * Ensures that the given object is present. If the object is null, an exception is thrown
   * with the specified message.
   *
   * <p>The thrown exception has context information about the node and graph.</p>
   *
   * @param obj the object to check for null
   * @param msg the message to include in the exception if the object is null
   * @throws PassError if the object is null
   */
  @Contract("null, _  -> fail")
  @FormatMethod
  protected final <T> T ensurePresent(@Nullable Optional<T> obj, String msg) {
    ensureNonNull(obj, "Optional must not be null");
    ensure(obj.isPresent(), msg);
    return obj.get();
  }

  public GeneralConfiguration configuration() {
    return configuration;
  }
}
