package vadl.error;

import java.util.ArrayList;
import java.util.List;
import vadl.utils.SourceLocation;


/**
 * A shared diagnostic instance for warnings and errors in the vadl system.
 *
 * <p>The diagnostics are used to report to the user that something went wrong or can be improved,
 * they are the main communication method between the vadl tool and user.
 *
 * <p><strong>Structure</strong>
 * The structure a diagnostic are strongly inspired by
 * <a href="https://rustc-dev-guide.rust-lang.org/diagnostics.html?highlight=diagnostic#diagnostic-structure">Rust's diagnostics</a>
 * which in our mind gets a lot of this right.
 * <pre>
 * error: Redeclaration of variable             // The level followed by the reason
 *     ╭──[file.vadl:6:19]                      // The file and line of the error
 *     │
 *   6 │ constant flo = 25                      // The primary location of the multilocation
 *     │          ^^^ Second usage              // A message attached to the primary location
 *     │              note: this isn't ok       // A "note" message attached to the primary location
 *     ⋮
 *   3 │ constant flo = 18                      // A secondary location of the multilocation
 *     │          --- First definition          // Which also has a message
 *     │
 *     Each variable can only be declared once. // Messages can also belong to the whole diagnostic
 *     help: Find another awesome name.
 * </pre>
 * While they can be thrown on their own, which makes sense in cases where the error is so severe
 * that execution cannot continue, it often makes sense to defer throwing and collect multiple
 * errors to give the user as much information as possible. In those cases the
 * {@link DiagnosticList} can store multiple diagnostics and throw them at once.
 */
public class Diagnostic extends RuntimeException {
  public final Level level;
  public final String reason;
  public final MultiLocation multiLocation;
  public final List<Message> messages;

  /**
   * It's generally recommended to not instantiate Diagnostics on their own but to make use of the
   * {@link Diagnostic#error(String, SourceLocation)} and
   * {@link Diagnostic#warning(String, SourceLocation)}
   * builders.
   */
  public Diagnostic(Level level, String reason, MultiLocation multiLocation,
                    List<Message> messages) {
    this.level = level;
    this.reason = reason;
    this.multiLocation = multiLocation;
    this.messages = messages;
  }

  /**
   * Creates a {@link DiagnosticBuilder} for an error that already has all the mandatory fields
   * filled in.
   *
   * @param reason   for the error.
   * @param location where the error occurred (primary location).
   * @return the builder.
   */
  public static DiagnosticBuilder error(String reason, SourceLocation location) {
    return new DiagnosticBuilder(Level.ERROR, reason, location);
  }


  /**
   * Creates a {@link DiagnosticBuilder} for a warning that already has all the mandatory fields
   * filled in.
   *
   * @param reason   for the warning.
   * @param location where the warning occurred (primary location).
   * @return the builder.
   */
  public static DiagnosticBuilder warning(String reason, SourceLocation location) {
    return new DiagnosticBuilder(Level.WARNING, reason, location);
  }

  @Override
  public String getMessage() {
    return level + " " + reason + " " + multiLocation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Diagnostic that = (Diagnostic) o;
    return level == that.level && reason.equals(that.reason)
        && multiLocation.equals(that.multiLocation) && messages.equals(that.messages);
  }

  @Override
  public int hashCode() {
    int result = level.hashCode();
    result = 31 * result + reason.hashCode();
    result = 31 * result + multiLocation.hashCode();
    result = 31 * result + messages.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return reason;
  }

  /**
   * A bundle of multiple location with messages attached to them.
   *
   * @param primaryLocation    where the warning/error occurred.
   * @param secondaryLocations many multiple locations to provide more context.
   */
  public record MultiLocation(LabeledLocation primaryLocation,
                              List<LabeledLocation> secondaryLocations
  ) {

    /**
     * Finds the labeled location of a plain location or inserts it.
     *
     * @param location to find or insert.
     * @return the found or created labled locaiton.
     */
    public LabeledLocation getOrInsert(SourceLocation location) {
      if (location.equals(primaryLocation.location)) {
        return primaryLocation;
      }

      var secondary =
          secondaryLocations.stream().filter(ll -> ll.location.equals(location)).findFirst();

      if (secondary.isPresent()) {
        return secondary.get();
      }

      var labeledLocation = new LabeledLocation(location, new ArrayList<>());
      secondaryLocations.add(labeledLocation);
      return labeledLocation;
    }
  }

  /**
   * A Location which is annotated with zero or more labels.
   *
   * @param location to be annotated
   * @param labels   to annotate
   */
  public record LabeledLocation(SourceLocation location, List<Message> labels) {
  }

  /**
   * A message (also includes help and notes) which can be part of the error itself or pinned to a
   * location.
   *
   * @param type    of the message
   * @param content the message says
   */
  public record Message(MsgType type, String content) {
  }

  /**
   * Help should be used to show changes the user can possibly make to fix the problem.
   * Note should be used for everything else, such as other context, information and facts,
   * online resources to read, etc.
   */
  public enum MsgType {
    NOTE,
    HELP,
    PLAIN
  }

  /**
   * The level/severity of a diagnostic.
   */
  public enum Level {
    ERROR,
    WARNING,
  }

  @Override
  public String getMessage() {
    // This is just used to see some information when using an other frontend than the
    // open-vadl one (namely the old one from the vadl project).
    // TODO: Add more infos
    return reason;
  }
}