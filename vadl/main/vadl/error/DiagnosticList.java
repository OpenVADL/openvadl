package vadl.error;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A general exception that something went wrong with a list of errors that caused the problem.
 */
public class DiagnosticList extends RuntimeException {
  public List<Diagnostic> items;

  public DiagnosticList(List<Diagnostic> items) {
    super(items.size() + " diagnostics(s):\n" + buildErrorMessage(items));
    this.items = items;
  }

  private static String buildErrorMessage(List<Diagnostic> errors) {
    return errors.stream().map(Diagnostic::getMessage).collect(Collectors.joining("\n"));
  }
}
