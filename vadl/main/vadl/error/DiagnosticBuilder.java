// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.error;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;

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
  public DiagnosticBuilder locationDescription(WithLocation location, String content,
                                               Object... args) {
    locationLabel(location.location(),
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
  public DiagnosticBuilder locationNote(WithLocation location, String content,
                                        Object... args) {
    locationLabel(location.location(),
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
  public DiagnosticBuilder locationHelp(WithLocation location, String content,
                                        Object... args) {
    locationLabel(location.location(),
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
