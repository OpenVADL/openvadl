package vadl.ast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.SourceLocation;

/**
 * A parser for the VADL language, generated using Coco.
 */
public class VadlParser {

  /**
   * Parses the VADL source program at the specified path into an AST.
   */
  public static Ast parse(Path path) throws IOException {
    var scanner = new Scanner(Files.newInputStream(path));
    var parser = new Parser(scanner);
    parser.sourceFile = path.toUri();
    return parse(parser);
  }

  /**
   * Parses the VADL source program at the specified path into an AST.
   * Works just like {@link VadlParser#parse(String, Map, URI)},
   * except errors will have the proper file locations set.
   */
  public static Ast parse(Path path, Map<String, String> macroOverrides) throws IOException {
    var scanner = new Scanner(Files.newInputStream(path));
    var parser = new Parser(scanner);
    parser.sourceFile = path.toUri();
    macroOverrides.forEach((key, value) -> parser.macroOverrides.put(key,
        new Identifier(value, SourceLocation.INVALID_SOURCE_LOCATION)));
    var ast = parse(parser);
    ast.fileUri = path.toUri();
    return ast;
  }

  /**
   * Convenience overload for {@link VadlParser#parse(String, Map, URI)} without any overrides.
   */
  public static Ast parse(String program) {
    return parse(program, Map.of(), null);
  }

  /**
   * Convenience overload for {@link VadlParser#parse(String, Map, URI)} without any overrides.
   */
  public static Ast parse(String program, URI resolutionUri) {
    return parse(program, Map.of(), resolutionUri);
  }

  /**
   * Parses a source program into an AST.
   *
   * @param program        a source code file to parse
   * @param macroOverrides The overrides to perform in the macro evaluation
   * @return The parsed syntax tree.
   * @throws DiagnosticList if there are any parsing errors.
   */
  public static Ast parse(String program, Map<String, String> macroOverrides,
                          @Nullable URI resolutionUri) {
    var scanner = new Scanner(new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)));
    var parser = new Parser(scanner);
    parser.resolutionUri = resolutionUri;
    parser.sourceFile = URI.create("memory://internal");
    macroOverrides.forEach((key, value) -> parser.macroOverrides.put(key,
        new Identifier(value, SourceLocation.INVALID_SOURCE_LOCATION)));
    return parse(parser);
  }

  private static Ast parse(Parser parser) {
    parser.ast.passTimings.add(new PassTimings(System.nanoTime(), "Start parsing"));
    // Setting up the Error printing, so we can parse it again.
    // This is mainly because coco/r doesn't give us access to the errors internally but always
    // want's to print them.
    var outStream = new ByteArrayOutputStream();
    parser.errors.errorStream = new PrintStream(outStream);
    parser.errors.errMsgFormat = "{0};{1};{2}";

    List<Diagnostic> errors = new ArrayList<>();


    try {
      parser.Parse();
      parser.ast.passTimings.add(new PassTimings(System.nanoTime(), "Syntax parsing"));
    } catch (Exception e) {
      errors.add(Diagnostic.error("Exception caught during parsing: " + e,
          SourceLocation.INVALID_SOURCE_LOCATION).build());
    }

    if (parser.errors.count > 0) {
      var lines = outStream.toString(StandardCharsets.UTF_8).split("\n", -1);
      for (var line : lines) {
        if (line.trim().isEmpty()) {
          continue;
        }

        var fields = line.split(";", 3);
        // Not every error has a location specified
        var lineNum = fields.length == 3 ? Integer.parseInt(fields[0]) : -1;
        var colNum = fields.length == 3 ? Integer.parseInt(fields[1]) : -1;
        var title = fields[fields.length - 1];
        var error = Diagnostic.error(
            title,
            new SourceLocation(parser.sourceFile, new SourceLocation.Position(lineNum, colNum))
        );
        if (title.contains("expected")) {
          error.note(
              "Sometimes the expected is just something with what the parser could work with "
                  + " but maybe not what you intended.");
        }
        errors.add(error.build());
      }
    }

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors.stream().distinct().toList());
    }

    var ast = parser.ast;

    errors.addAll(SymbolTable.ResolutionPass.resolveSymbols(ast));

    if (!errors.isEmpty()) {
      throw new DiagnosticList(errors.stream().distinct().toList());
    }

    return ast;
  }

  /**
   * Reads the pass timings persisted in the AST and prints them to stdout
   * in a human-readable way.
   *
   * @param ast The AST whose pass timings should be printed
   */
  public static void printPassTimings(Ast ast) {
    PassTimings prev = null;
    for (var timing : ast.passTimings) {
      if (prev != null) {
        double deltaMillis = (timing.timestamp - prev.timestamp) / 1000_000.0;
        System.out.printf("%s - %.3f ms\n", timing.description, deltaMillis);
      }
      prev = timing;
    }
  }

  record PassTimings(long timestamp, String description) {
  }
}
