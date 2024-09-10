package vadl.viam;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import vadl.error.VadlError;
import vadl.utils.SourceLocation;

/**
 * The {@link ViamError} indicates unintended failures during
 * VIAM related processing.
 *
 * <p>It provides additional context information.
 */
public class ViamError extends RuntimeException {

  private final List<String> context = new ArrayList<>();
  private SourceLocation location = SourceLocation.INVALID_SOURCE_LOCATION;

  public ViamError(String message) {
    super(message);
  }

  @FormatMethod
  public ViamError(String message, Object... args) {
    super(message.formatted(args));
  }

  public ViamError(String message, Throwable cause) {
    super(message, cause);
  }

  public SourceLocation location() {
    return location;
  }

  public ViamError addLocation(SourceLocation location) {
    this.location = location;
    return this;
  }

  public ViamError addContext(String context) {
    this.context.add(context);
    return this;
  }

  /**
   * Adds additional context information to the {@link ViamError} object.
   *
   * @param definition The definition object to add as context.
   * @return The updated ViamError object with added context.
   */
  public ViamError addContext(Definition definition) {
    return this.addContext("name", definition.identifier.name())
        .addContext("definition", definition)
        .addLocation(definition.sourceLocation());

  }

  @FormatMethod
  public ViamError addContext(String format, Object... args) {
    this.context.add(String.format(format, args));
    return this;
  }

  public ViamError addContext(String name, Object arg) {
    this.context.add(String.format("%s:\t%s", name, arg));
    return this;
  }

  public VadlError toVadlError() {
    return new VadlError(getMessage(), location, null, null);
  }

  /**
   * Returns the context as a formatted String.
   */
  public String context() {
    return context.stream()
        .map("\n\twith %s"::formatted)
        .collect(Collectors.joining())
        + "\n\twith location:\t%s".formatted(location)
        + "\n\twith source:\t%s".formatted(location.toSourceString());
  }


  /**
   * Removes the upper {@code n} stacktrace entries.
   * This is useful if helper methods create exceptions but are not
   * responsible for it.
   */
  public ViamError shrinkStacktrace(int n) {
    StackTraceElement[] stackTrace = this.getStackTrace();
    if (stackTrace.length > n) {
      StackTraceElement[] newStackTrace = Arrays.copyOfRange(stackTrace, n, stackTrace.length);
      this.setStackTrace(newStackTrace);
    }
    return this;
  }

  @Override
  public String getMessage() {
    return "%s%s".formatted(super.getMessage(), context());
  }

  /**
   * Get the message but without the context. This method is useful to assert the message in tests.
   */
  @Nullable
  public String getContextlessMessage() {
    return super.getMessage();
  }

  /**
   * Ensure that a condition is true, otherwise throw a ViamError with a formatted error message.
   *
   * @param condition the condition to check
   * @param format    the format string for the error message
   * @param args      the arguments to format the error message
   * @throws ViamError if the condition is false
   */
  @Contract("false, _, _ -> fail")
  @FormatMethod
  public static void ensure(boolean condition, String format, Object... args) {
    if (!condition) {
      throw new ViamError(format.formatted(args))
          .shrinkStacktrace(1);
    }
  }

}
