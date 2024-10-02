package vadl.error;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * A storage where diagnostics are added to which shouldn't interrupt the program flow but which
 * should nonetheless be presented to the programmer at the end.
 *
 * <p>In case you want to interrupt the program flow you can simply throw a {@link Diagnostic} or
 * a {@link DiagnosticList}.
 */
public class DeferredDiagnosticStore {
  private static final LinkedHashSet<Diagnostic> diagnosticList = new LinkedHashSet<>();

  /**
   * Add another diagnostic to the store.
   *
   * <p>If the diagnostic is already in the store it won't be inserted a second time.
   */
  public static void add(Diagnostic diagnostic) {
    diagnosticList.add(diagnostic);
  }

  /**
   * Retrieves all diagnostics in the order they were inserted.
   */
  public static List<Diagnostic> getAll() {
    return diagnosticList.stream().toList();
  }

  public static boolean isEmpty() {
    return diagnosticList.isEmpty();
  }
}
