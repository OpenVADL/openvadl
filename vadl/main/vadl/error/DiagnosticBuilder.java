package vadl.error;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import vadl.utils.SourceLocation;
import vadl.utils.WithSourceLocation;

/**
 * An ergonomic builder for a diagnostic.
 *
 * <p>Diagnostics can hold a lot of information making them quite powerful, but also cumbersome to
 * create since often not all of their features are needed.
 */
public class DiagnosticBuilder extends Throwable {
  private final Diagnostic diagnostic;

  /**
   * Create a ergonomic builder for a diagnostic.
   *
   * @param level    for the diagnostic.
   * @param reason   for the diagnostic.
   * @param location for the diagnostic.
   */
  DiagnosticBuilder(Diagnostic.Level level, String reason, SourceLocation location) {
    this.diagnostic = new Diagnostic(
        level,
        reason,
        new Diagnostic.MultiLocation(
            new Diagnostic.LabeledLocation(location, new ArrayList<>()), new ArrayList<>()
        ),
        new ArrayList<>()
    );
  }

  private void locationLabel(SourceLocation location, Diagnostic.Message message) {
    diagnostic.multiLocation.getOrInsert(location).labels().add(message);
  }

  /**
   * Attaches a description to a location. If the location doesn't exist, yet it will add the
   * location first as a secondary location.
   *
   * @param location to which the description will be attached.
   * @param content  of the description.
   * @return the builder itself.
   */
  @FormatMethod
  public DiagnosticBuilder locationDescription(WithSourceLocation location, String content,
                                               Object... args) {
    locationLabel(location.sourceLocation(),
        new Diagnostic.Message(Diagnostic.MsgType.PLAIN, content.formatted(args)));
    return this;
  }

  /**
   * Attaches a note to a location. If the location doesn't exist, yet it will add the
   * location first as a secondary location.
   *
   * @param location to which the note will be attached.
   * @param content  of the note.
   * @return the builder itself.
   */
  @FormatMethod
  public DiagnosticBuilder locationNote(WithSourceLocation location, String content,
                                        Object... args) {
    locationLabel(location.sourceLocation(),
        new Diagnostic.Message(Diagnostic.MsgType.NOTE, content.formatted(args)));
    return this;
  }

  /**
   * Attaches a help message to a location. If the location doesn't exist, yet it will add the
   * location first as a secondary location.
   *
   * @param location to which the help message will be attached.
   * @param content  of the help message.
   * @return the builder itself.
   */
  @FormatMethod
  public DiagnosticBuilder locationHelp(WithSourceLocation location, String content,
                                        Object... args) {
    locationLabel(location.sourceLocation(),
        new Diagnostic.Message(Diagnostic.MsgType.HELP, content.formatted(args)));
    return this;
  }

  /**
   * Attaches a plain description to the error.
   *
   * @param content of the description.
   * @return the builder itself.
   */
  @FormatMethod
  public DiagnosticBuilder description(String content, Object... args) {
    diagnostic.messages.add(new Diagnostic.Message(
        Diagnostic.MsgType.PLAIN,
        content.formatted(args)
    ));
    return this;
  }

  /**
   * Attaches a note to the error.
   *
   * @param content of the note.
   * @return the builder itself.
   */
  @FormatMethod
  public DiagnosticBuilder note(String content, Object... args) {
    diagnostic.messages.add(new Diagnostic.Message(
        Diagnostic.MsgType.NOTE,
        content.formatted(args)
    ));
    return this;
  }

  /**
   * Attaches a help message to the error.
   *
   * @param content of the help message.
   * @return the builder itself.
   */
  @FormatMethod
  public DiagnosticBuilder help(String content, Object... args) {
    diagnostic.messages.add(new Diagnostic.Message(
        Diagnostic.MsgType.HELP,
        content.formatted(args)
    ));
    return this;
  }

  public Diagnostic build() {
    return diagnostic;
  }
}
