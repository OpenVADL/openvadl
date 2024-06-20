package vadl.pass.exception;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;

public class PassError extends RuntimeException {

  private final List<String> context = new ArrayList<>();

  @FormatMethod
  public PassError(String message, @Nullable Object... args) {
    super(message.formatted(args));
  }

  public PassError shrinkStacktrace(int n) {
    StackTraceElement[] stackTrace = this.getStackTrace();
    if (stackTrace.length > n) {
      StackTraceElement[] newStackTrace = Arrays.copyOfRange(stackTrace, n, stackTrace.length);
      this.setStackTrace(newStackTrace);
    }
    return this;
  }
}
