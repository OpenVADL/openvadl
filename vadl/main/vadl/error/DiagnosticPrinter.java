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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import vadl.utils.SourceLocation;

/**
 * A human focused command line printer for vadl diagnostics.
 */
public class DiagnosticPrinter {

  private final PrinterColors colors;
  private final Map<Path, List<String>> fileLineCache = new HashMap<>();

  public DiagnosticPrinter() {
    this(true);
  }

  public DiagnosticPrinter(boolean enableColors) {
    colors = enableColors ? new AnsiColors() : new NoColors();
  }


  /**
   * Prints a list of diagnostics into a string.
   *
   * @param diagnosticList to print.
   */
  public String toString(List<Diagnostic> diagnosticList) {
    StringBuilder builder = new StringBuilder();
    diagnosticList.forEach(d -> toString(d, builder));
    return builder.toString();
  }

  /**
   * Prints a list of diagnostics into a string.
   *
   * @param diagnosticList to print.
   */
  public String toString(DiagnosticList diagnosticList) {
    return toString(diagnosticList.items);
  }

  /**
   * Prints a diagnostic into a string.
   *
   * @param diagnostic to print.
   */
  public String toString(Diagnostic diagnostic) {
    var builder = new StringBuilder();
    toString(diagnostic, builder);
    return builder.toString();
  }

  /**
   * Prints a diagnostic into a string.
   *
   * @param diagnostic to print.
   */
  private void toString(Diagnostic diagnostic, StringBuilder builder) {
    printHeader(diagnostic, builder);
    printMultiSourcePreview(diagnostic, builder);
    builder.append(indentBy(messageBlock(diagnostic.messages), "     "));
    builder.append("\n");
  }

  /**
   * Prints the header which includes the type of diagnostic that was thrown.
   *
   * @param diagnostic for which the header should be printed.
   */
  private void printHeader(Diagnostic diagnostic, StringBuilder builder) {
    switch (diagnostic.level) {
      case ERROR -> builder.append(
          "%s%serror:%s%s %s%s\n".formatted(colors.bold(), colors.red(), colors.reset(),
              colors.bold(),
              diagnostic.reason, colors.reset()));
      case WARNING ->
          builder.append("%s%swarning:%s%s %s%s\n".formatted(colors.bold(), colors.yellow(),
              colors.reset(),
              colors.bold(),
              diagnostic.reason, colors.reset()));
      default -> throw new IllegalStateException();
    }
  }

  /**
   * Print all (one or more) previews of the diagnostic.
   *
   * <p>A single diagnostic has one but possible multiple locations attached with it. This
   * method prints all of them, including the locations that arose from macro expansions.
   *
   * @param diagnostic to print.
   */
  private void printMultiSourcePreview(Diagnostic diagnostic, StringBuilder builder) {
    // Print preview header
    builder.append("     %s╭──[%s]\n".formatted(colors.cyan(),
        diagnostic.multiLocation.primaryLocation().location().toIDEString()));
    builder.append("     │\n");

    // TODO: Sort them accordig to their line number in the future
    var allSnippets = new ArrayList<>(diagnostic.multiLocation.secondaryLocations());
    allSnippets.add(diagnostic.multiLocation.primaryLocation());

    for (int i = 0; i < allSnippets.size(); i++) {
      var snippet = allSnippets.get(i);

      // FIXME: if the first location is not from the same file as the primary we don't currently
      //        the file it is from.

      // Delimiter in multilocation
      if (i > 0) {
        var previous = allSnippets.get(i - 1);
        builder.append("\n");
        builder.append(sourceDelimiter(previous.location(), snippet.location()));
      }

      printExpandedSourcePreview(snippet,
          snippet.equals(diagnostic.multiLocation.primaryLocation()), builder);
    }
    builder.append("\n     %s│ %s\n".formatted(colors.cyan(), colors.reset()));
  }

  /**
   * Print the delimiter between two source previews (within a multipreview).
   *
   * @param previous location preceding the delimiter.
   * @param next     location following the delimiter.
   */
  private String sourceDelimiter(SourceLocation previous,
                                 SourceLocation next) {
    if (!previous.uri().equals(next.uri())) {
      // This is so unusual that we print the location everytime
      var message = "     %s⋮\n".formatted(colors.cyan());
      message += "     ╭─ %s\n".formatted(next.toIDEString());
      return message;
    } else if (next.begin().line() == previous.end().line() + 1) {
      return "     %s│\n".formatted(colors.cyan());
    } else {
      return "     %s⋮\n".formatted(colors.cyan());
    }
  }

  /**
   * Print a location and the chain of expansions from which it originated.
   *
   * @param location  to print.
   * @param isPrimary whether it's the main reason of the diagnostic.
   * @param builder   into which the preview will be printed.
   */
  private void printExpandedSourcePreview(Diagnostic.LabeledLocation location, boolean isPrimary,
                                          StringBuilder builder) {
    // Print the original preview
    printSourcePreview(location, isPrimary, builder);

    // Print also the chain/stack of the location from which this error was expanded form
    var last = location.location();
    var next = location.location().expandedFrom();
    while (next != null) {
      builder.append("\n");
      builder.append(sourceDelimiter(last, next));
      printSourcePreview(
          new Diagnostic.LabeledLocation(
              next,
              List.of(
                  new Diagnostic.Message(Diagnostic.MsgType.PLAIN, "from this model invocation"))),
          false, builder);
      last = next;
      next = next.expandedFrom();
    }
  }

  /**
   * Print a single source location preview (with inline messages) into a string builder.
   *
   * @param location  to print.
   * @param isPrimary indicating the priority.
   * @param builder   into which will be printed.
   */
  private void printSourcePreview(Diagnostic.LabeledLocation location, boolean isPrimary,
                                  StringBuilder builder) {
    List<String> lines;
    try {
      lines = getFileLines(location.location().uri());
    } catch (IOException | IllegalArgumentException e) {
      var previewError = "No Preview available: Could not find the file '%s'".formatted(
          location.location().uri()
      );
      var prefix = "     %s│%s    ".formatted(colors.cyan(), colors.reset());
      var text = "%s%s%s\n%s".formatted(colors.yellow(), previewError, colors.reset(),
          messageBlock(location));
      builder.append(indentBy(text, prefix));
      return;
    }

    if (location.location().equals(SourceLocation.INVALID_SOURCE_LOCATION)) {
      var previewError = "No Preview available: The location was lost.";
      var prefix = "     %s│%s    ".formatted(colors.cyan(), colors.reset());
      var text = "%s%s%s\n%s".formatted(colors.yellow(), previewError, colors.reset(),
          messageBlock(location));
      builder.append(indentBy(text, prefix));
      return;
    }


    if (location.location().begin().line() < 1
        || location.location().begin().column() < 1
        || location.location().end().line() > lines.size()
        || location.location().end().line() < location.location().begin().line()
    ) {
      var previewError =
          "No Preview available: The location was corrupted "
              + "(line or column are out of the range) %s".formatted(location.location());
      var prefix = "     %s│%s    ".formatted(colors.cyan(), colors.reset());
      var text = "%s%s%s\n%s".formatted(colors.yellow(), previewError, colors.reset(),
          messageBlock(location));
      builder.append(indentBy(text, prefix));
      return;
    }

    // Printing the Source line
    if (location.location().begin().line() != location.location().end().line()) {
      printMultiLinePreview(location, isPrimary, lines, builder);
    } else {
      printSingleLinePreview(location, isPrimary, lines, builder);
    }
  }


  /**
   * Prints a multi line preview, with highlighting and inline messages to the provided builder.
   *
   * @param location  to be printed.
   * @param isPrimary indicates the importance of the error.
   * @param lines     cache.
   * @param builder   to be printed to.
   */
  private void printMultiLinePreview(Diagnostic.LabeledLocation location, boolean isPrimary,
                                     List<String> lines, StringBuilder builder) {

    var numLines = location.location().end().line() - location.location().begin().line() + 1;

    // For multiline segments up until 7 lines we print everything,
    // Otherwise we only print the first 3 and last 3 lines.
    if (numLines < 8) {
      for (int i = location.location().begin().line(); i <= location.location().end().line(); i++) {
        // Print the line number, guard and actual line
        builder.append("%s%4d".formatted(colors.reset(), i));
        builder.append(
            " %s│%s>%s ".formatted(colors.cyan(), isPrimary ? colors.red() : colors.lightblue(),
                colors.reset()));
        builder.append(lines.get(i - 1));
        builder.append("\n");
      }
    } else {
      for (int i = location.location().begin().line(); i <= location.location().begin().line() + 2;
           i++) {
        // Print the line number, guard and actual line
        builder.append("%s%4d".formatted(colors.reset(), i));
        builder.append(" %s│%s>%s ".formatted(colors.cyan(), colors.red(), colors.reset()));
        builder.append(lines.get(i - 1));
        builder.append("\n");
      }
      builder.append(
          "    %s⋮%s  %d lines omitted here...\n".formatted(colors.cyan(), colors.reset(),
              numLines - 6));
      for (int i = location.location().end().line() - 2; i <= location.location().end().line();
           i++) {
        // Print the line number, guard and actual line
        builder.append("%s%4d".formatted(colors.reset(), i));
        builder.append(" %s│%s>%s ".formatted(colors.cyan(), colors.red(), colors.reset()));
        builder.append(lines.get(i - 1));
        builder.append("\n");
      }
    }

    var prefix = "     %s│%s    ".formatted(colors.cyan(), colors.reset());
    builder.append(indentBy(messageBlock(location), prefix));
  }

  /**
   * Prints a single line preview, with highlighting and inline messages to the provided builder.
   *
   * @param location  to be printed.
   * @param isPrimary indicates the importance of the error.
   * @param lines     cache.
   * @param builder   to be printed to.
   */
  private void printSingleLinePreview(Diagnostic.LabeledLocation location, boolean isPrimary,
                                      List<String> lines, StringBuilder builder) {

    // Print the line number, guard and actual line
    builder.append("%s%4d".formatted(colors.reset(), location.location().begin().line()));
    builder.append(" %s│%s ".formatted(colors.cyan(), colors.reset()));
    builder.append(lines.get(location.location().begin().line() - 1));
    builder.append("\n");

    // Print the underlining
    var highlightLength =
        location.location().end().column() - location.location().begin().column() + 1;
    var highlightPadding = isPrimary
        ? colors.red() + "^".repeat(highlightLength) + colors.reset()
        : colors.lightblue() + "-".repeat(highlightLength) + colors.reset();
    var nonHighlightPadding = " ".repeat(highlightLength);
    var padding = "     %s│%s ".formatted(colors.cyan(), colors.reset())
        + " ".repeat(location.location().begin().column() - 1);

    // Generate the complete block, with highlighting and messages.
    var textBlock = messageBlock(location);
    var highlightedBlock =
        indentFirstBy(textBlock, highlightPadding + " ", nonHighlightPadding + " ");
    var completedBlock = indentBy(highlightedBlock, padding);

    builder.append(completedBlock);
  }

  private String messageBlock(Diagnostic.LabeledLocation location) {
    return messageBlock(location.labels());
  }

  private String messageBlock(List<Diagnostic.Message> messages) {
    var builder = new StringJoiner("\n");
    messages.forEach(m -> builder.add(messageLine(m)));
    return builder.toString();
  }

  private String messageLine(Diagnostic.Message message) {
    var label = switch (message.type()) {
      case PLAIN -> "";
      case NOTE -> "note: ";
      case HELP -> "help: ";
    };

    return "%s%s%s%s".formatted(colors.bold(), label, colors.reset(), message.content());
  }

  /**
   * Indents all lines but uses a different indent string for the first line.
   *
   * @param text        to indent.
   * @param firstPrefix to indent the first line with.
   * @param otherPrefix to indent the other lines with.
   * @return the indented string (text block).
   */
  private String indentFirstBy(String text, String firstPrefix, String otherPrefix) {
    return firstPrefix + text.replaceAll("\n", "\n" + otherPrefix);
  }

  /**
   * Indents all lines in a multiline string with the provided prefix.
   *
   * @param text   to indent.
   * @param prefix to indent with.
   * @return the indented string (text block).
   */
  private String indentBy(String text, String prefix) {
    return prefix + text.replaceAll("\n", "\n" + prefix);
  }

  /**
   * Get all lines fo a file.
   *
   * @param uri of the file to load
   * @return list of lines.
   * @throws IOException if the file doesn't exist.
   */
  private List<String> getFileLines(URI uri) throws IOException {
    var path = new File(uri).toPath();
    if (fileLineCache.containsKey(path)) {
      return fileLineCache.get(path);
    }

    var lines = Files.readAllLines(new File(uri).toPath(), Charset.defaultCharset());
    fileLineCache.put(path, lines);
    return lines;
  }

  @SuppressWarnings("UnusedMethod")
  private interface PrinterColors {
    String reset();

    String bold();

    String underline();

    String black();

    String red();

    String green();

    String orange();

    String blue();

    String purple();

    String cyan();

    String lightgrey();

    String darkgrey();

    String lightred();

    String lightgreen();

    String yellow();

    String lightblue();

    String pink();

    String lightcyan();
  }

  private static class AnsiColors implements PrinterColors {
    @Override
    public String reset() {
      return "\033[0m";
    }

    @Override
    public String bold() {
      return "\033[01m";
    }

    @Override
    public String underline() {
      return "\033[04m";
    }

    @Override
    public String black() {
      return "\033[30m";
    }

    @Override
    public String red() {
      return "\033[31m";
    }

    @Override
    public String green() {
      return "\033[32m";
    }

    @Override
    public String orange() {
      return "\033[33m";
    }

    @Override
    public String blue() {
      return "\033[34m";
    }

    @Override
    public String purple() {
      return "\033[35m";
    }

    @Override
    public String cyan() {
      return "\033[36m";
    }

    @Override
    public String lightgrey() {
      return "\033[37m";
    }

    @Override
    public String darkgrey() {
      return "\033[90m";
    }

    @Override
    public String lightred() {
      return "\033[91m";
    }

    @Override
    public String lightgreen() {
      return "\033[92m";
    }

    @Override
    public String yellow() {
      return "\033[93m";
    }

    @Override
    public String lightblue() {
      return "\033[94m";
    }

    @Override
    public String pink() {
      return "\033[95m";
    }

    @Override
    public String lightcyan() {
      return "\033[96m";
    }
  }

  private static class NoColors implements PrinterColors {
    @Override
    public String reset() {
      return "";
    }

    @Override
    public String bold() {
      return "";
    }

    @Override
    public String underline() {
      return "";
    }

    @Override
    public String black() {
      return "";
    }

    @Override
    public String red() {
      return "";
    }

    @Override
    public String green() {
      return "";
    }

    @Override
    public String orange() {
      return "";
    }

    @Override
    public String blue() {
      return "";
    }

    @Override
    public String purple() {
      return "";
    }

    @Override
    public String cyan() {
      return "";
    }

    @Override
    public String lightgrey() {
      return "";
    }

    @Override
    public String darkgrey() {
      return "";
    }

    @Override
    public String lightred() {
      return "";
    }

    @Override
    public String lightgreen() {
      return "";
    }

    @Override
    public String yellow() {
      return "";
    }

    @Override
    public String lightblue() {
      return "";
    }

    @Override
    public String pink() {
      return "";
    }

    @Override
    public String lightcyan() {
      return "";
    }
  }
}
