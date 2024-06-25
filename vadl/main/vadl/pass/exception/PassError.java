package vadl.pass.exception;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

/**
 * This exception represents an error which happened during the pass execution.
 */
public class PassError extends RuntimeException {

  private final List<String> context = new ArrayList<>();

  @FormatMethod
  public PassError(String message, @Nullable Object... args) {
    super(message.formatted(args));
  }

  /**
   * Removes the upper {@code n} stacktrace entries.
   * This is useful if helper methods create exceptions but are not
   * responsible for it.
   */
  public PassError shrinkStacktrace(int n) {
    StackTraceElement[] stackTrace = this.getStackTrace();
    if (stackTrace.length > n) {
      StackTraceElement[] newStackTrace = Arrays.copyOfRange(stackTrace, n, stackTrace.length);
      this.setStackTrace(newStackTrace);
    }
    return this;
  }
}
