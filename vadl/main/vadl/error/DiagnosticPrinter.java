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
import vadl.utils.SourceLocation;

/**
 * A human focused command line printer for vadl diagnostics.
 */
public class DiagnosticPrinter {

  Map<Path, List<String>> fileCache = new HashMap<>();

  /**
   * Prints a list of diagnostics to stdout.
   *
   * @param diagnosticList to print.
   */
  public void print(List<Diagnostic> diagnosticList) {
    for (var item : diagnosticList) {
      print(item);
    }
  }

  /**
   * Prints a list of diagnostics to stdout.
   *
   * @param diagnosticList to print.
   */
  public void print(DiagnosticList diagnosticList) {
    print(diagnosticList.items);
  }

  /**
   * Prints an error to stdout.
   *
   * @param diagnostic to print.
   */
  public void print(Diagnostic diagnostic) {
    printHeader(diagnostic);

    printMultiSourcePreview(diagnostic);

    for (var message : diagnostic.messages) {
      System.out.print("    ");
      printMessage(message, "    ", 80 - 4);
    }
    System.out.printf("%s\n\n", Ansi.Reset);
  }

  private void printHeader(Diagnostic diagnostic) {
    switch (diagnostic.level) {
      case ERROR ->
          System.out.printf("%s%serror:%s%s %s%s\n", Ansi.Bold, Ansi.Red, Ansi.Reset, Ansi.Bold,
              diagnostic.reason, Ansi.Reset);
      case WARNING ->
          System.out.printf("%s%swarning:%s%s %s%s\n", Ansi.Bold, Ansi.Yellow, Ansi.Reset,
              Ansi.Bold,
              diagnostic.reason, Ansi.Reset);
      default -> throw new IllegalStateException();
    }
  }

  private void printMultiSourcePreview(Diagnostic diagnostic) {
    // Print preview header
    System.out.printf("    %s╭──[%s]\n", Ansi.Cyan,
        diagnostic.multiLocation.primaryLocation().location().toIDEString());
    System.out.println("    │");

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
        printSourceDelimiter(previous.location(), snippet.location());
      }

      printSourcePreview(snippet, snippet.equals(diagnostic.multiLocation.primaryLocation()));
    }
    System.out.printf("    %s│ %s\n", Ansi.Cyan, Ansi.Reset);
  }

  private void printSourceDelimiter(SourceLocation previous,
                                    SourceLocation next) {
    if (!previous.uri()
        .equals(next.uri())) {
      // This is so unusual that we print the location evertime
      System.out.printf("    %s⋮\n", Ansi.Cyan);
      System.out.printf("    ╭─ %s\n", next.toIDEString());
    } else if (next.begin().line() == previous.end().line() + 1) {
      System.out.printf("    %s│\n", Ansi.Cyan);
    } else {
      System.out.printf("    %s⋮\n", Ansi.Cyan);
    }
  }

  private void printSourcePreview(Diagnostic.LabeledLocation location, boolean isPrimary) {
    List<String> lines;
    try {
      lines = getFileLines(location.location().uri());
    } catch (IOException | IllegalArgumentException e) {
      System.out.printf("    │ %sNo Preview available: Could not find the file '%s'\n", Ansi.Reset,
          location.location().uri());
      return;
    }

    if (location.location().equals(SourceLocation.INVALID_SOURCE_LOCATION)) {
      System.out.printf("    │ %sThe location was lost.\n", Ansi.Reset);
      return;
    }
    if (location.location().begin().line() != location.location().end().line()) {
      System.out.printf("    │ %sMultiline preview not yet implemented\n", Ansi.Reset);
      return;
    }

    // Printing the Source line
    System.out.printf("%s%3d", Ansi.Reset, location.location().begin().line());
    System.out.printf(" %s│%s ", Ansi.Cyan, Ansi.Reset);
    System.out.printf("%s\n", lines.get(location.location().begin().line() - 1));

    // Printing the highlighting
    var highlightLength =
        location.location().end().column() - location.location().begin().column() + 1;
    var highlight = isPrimary
        ? Ansi.Red + "^".repeat(highlightLength) + Ansi.Reset
        : Ansi.Lightblue + "^".repeat(highlightLength) + Ansi.Reset;
    var padding = " ".repeat(location.location().begin().column() - 1);
    System.out.printf("    %s│ %s%s%s ", Ansi.Cyan, padding, highlight, Ansi.Reset);

    // Print the inline messages
    for (int i = 0; i < location.labels().size(); i++) {
      var label = location.labels().get(i);

      var prefix = "    %s│ %s%s".formatted(Ansi.Cyan, Ansi.Reset,
          " ".repeat(location.location().end().column() + 1));

      if (i > 0) {
        System.out.print(prefix);
      }
      int startCol = 6 + location.location().end().column();
      printMessage(label, prefix, 80 - startCol);
    }

    if (location.labels().isEmpty()) {
      System.out.println();
    }


    if (location.location().expandedFrom() != null) {
      printSourceDelimiter(location.location(), location.location().expandedFrom());
      printSourcePreview(
          new Diagnostic.LabeledLocation(
              location.location().expandedFrom(),
              List.of(
                  new Diagnostic.Message(Diagnostic.MsgType.PLAIN, "from this model invocation"))),
          false);
    }

  }


  private void printMessage(Diagnostic.Message message, String prefix, int maxWidth) {
    var label = switch (message.type()) {
      case PLAIN -> "";
      case NOTE -> "note: ";
      case HELP -> "help: ";
    };

    String indentPrefix = prefix + " ".repeat(label.length());
    var text =
        indentLines(wrapLines(message.content(), maxWidth - label.length()), indentPrefix);
    System.out.printf("%s%s%s%s\n", Ansi.Bold, label, Ansi.Reset, text);
  }

  private List<String> getFileLines(URI uri) throws IOException {
    var path = new File(uri).toPath();
    if (fileCache.containsKey(path)) {
      return fileCache.get(path);
    }

    var lines = Files.readAllLines(new File(uri).toPath(), Charset.defaultCharset());
    fileCache.put(path, lines);
    return lines;
  }

  private String indentLines(String text, String prefix) {
    return text.replaceAll("\n", "\n" + prefix);
  }

  private String wrapLines(String text, int maxWidth) {
    int lastWhitespace = 0;
    int width = 0;
    var builder = new StringBuilder(text);
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      width += 1;
      if (c == '\n') {
        width = 0;
        lastWhitespace = 0;
      }

      if (c == ' ') {
        lastWhitespace = i;
      }

      if (width >= maxWidth && lastWhitespace > 0) {
        builder.replace(lastWhitespace, lastWhitespace + 1, "\n");
        width = 0;
        lastWhitespace = 0;
      }
    }

    return builder.toString();
  }

  static class Ansi {
    static String Reset = "\033[0m";
    static String Bold = "\033[01m";
    static String Underline = "\033[04m";
    static String Black = "\033[30m";
    static String Red = "\033[31m";
    static String Green = "\033[32m";
    static String Orange = "\033[33m";
    static String Blue = "\033[34m";
    static String Purple = "\033[35m";
    static String Cyan = "\033[36m";
    static String Lightgrey = "\033[37m";
    static String Darkgrey = "\033[90m";
    static String Lightred = "\033[91m";
    static String Lightgreen = "\033[92m";
    static String Yellow = "\033[93m";
    static String Lightblue = "\033[94m";
    static String Pink = "\033[95m";
    static String Lightcyan = "\033[96m";
  }
}
