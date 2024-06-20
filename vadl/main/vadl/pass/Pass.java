package vadl.pass;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;
import org.jetbrains.annotations.Contract;
import vadl.pass.exception.PassError;
import vadl.viam.Specification;
import vadl.viam.graph.ViamGraphError;

/**
 * A pass is a unit of execution. It analysis or transforms a VADL specification.
 */
public abstract class Pass {
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
  public abstract Object execute(final Map<PassKey, Object> passResults, Specification viam)
      throws IOException;

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
   * @throws ViamGraphError if the condition is false
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
   * Ensures that the given object is not null. If the object is null, an exception is thrown
   * with the specified message.
   *
   * <p>The thrown exception has context information about the node and graph.</p>
   *
   * @param obj the object to check for null
   * @param msg the message to include in the exception if the object is null
   * @throws ViamGraphError if the object is null
   */
  @Contract("null, _  -> fail")
  @FormatMethod
  protected final void ensureNonNull(@Nullable Object obj, String msg) {
    ensure(obj != null, msg);
  }
}
