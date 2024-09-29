package vadl.error;

import java.util.ArrayList;
import java.util.List;

/**
 * A storage where diagnostics are added to which shouldn't interrupt the program flow but which
 * should nonetheless be presented to the programmer at the end.
 *
 * <p>In case you want to interrupt the program flow you can simply throw a {@link Diagnostic} or
 * a {@link DiagnosticList}.
 */
public class DeferredDiagnosticStore {
  private static final List<Diagnostic> diagnosticList = new ArrayList<Diagnostic>();

  public static void add(Diagnostic diagnostic) {
    diagnosticList.add(diagnostic);
  }

  public static List<Diagnostic> getAll() {
    return diagnosticList;
  }

  public static boolean isEmpty() {
    return diagnosticList.isEmpty();
  }
}
