package vadl.ast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vadl.error.VadlError;
import vadl.error.VadlException;
import vadl.utils.SourceLocation;

/**
 * A parser for the VADL language, generated using Coco.
 */
public class VadlParser {

  /**
   * Parses the VADL source program at the specified path into an AST.
   * Works just like {@link VadlParser#parse(String, Map)},
   * except errors will have the proper file locations set.
   */
  public static Ast parse(Path path, Map<String, String> macroOverrides) throws IOException {
    var scanner = new Scanner(Files.newInputStream(path));
    var parser = new Parser(scanner);
    parser.sourceFile = path.toUri();
    macroOverrides.forEach((key, value) -> parser.macroOverrides.put(key,
        new Identifier(value, SourceLocation.INVALID_SOURCE_LOCATION)));
    return parse(parser);
  }

  /**
   * Convenience overload for {@link VadlParser#parse(String, Map)} without any overrides.
   */
  public static Ast parse(String program) {
    return parse(program, Map.of());
  }

  /**
   * Parses a source program into an AST.
   *
   * @param program a source code file to parse
   * @param macroOverrides The overrides to perform in the macro evaluation
   * @return The parsed syntax tree.
   * @throws VadlException if there are any parsing errors.
   */
  public static Ast parse(String program, Map<String, String> macroOverrides) {
    var scanner = new Scanner(new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)));
    var parser = new Parser(scanner);
    macroOverrides.forEach((key, value) -> parser.macroOverrides.put(key,
        new Identifier(value, SourceLocation.INVALID_SOURCE_LOCATION)));
    return parse(parser);
  }

  private static Ast parse(Parser parser) {
    // Setting up the Error printing, so we can parse it again.
    // This is mainly because coco/r doesn't give us access to the errors internally but always
    // want's to print them.
    var outStream = new ByteArrayOutputStream();
    parser.errors.errorStream = new PrintStream(outStream);
    parser.errors.errMsgFormat = "{0};{1};{2}";

    List<VadlError> errors = new ArrayList<>();

    try {
      parser.Parse();
    } catch (Exception e) {
      e.printStackTrace();
      errors.add(new VadlError("Exception caught during parsing: " + e,
          SourceLocation.INVALID_SOURCE_LOCATION, null, null));
    }

    if (parser.errors.count > 0) {
      var lines = outStream.toString(StandardCharsets.UTF_8).split("\n", -1);
      for (var line : lines) {
        if (line.trim().isEmpty()) {
          continue;
        }

        var fields = line.split(";", 3);
        var lineNum = Integer.parseInt(fields[0]);
        var colNum = Integer.parseInt(fields[1]);
        var title = fields[2];
        errors.add(new VadlError(
            fields[2],
            new SourceLocation(parser.sourceFile,
                new SourceLocation.Position(lineNum, colNum)),
            null,
            title.contains("expected")
                ? "Sometimes the expected is just something with what the parser could work with "
                + " but maybe not what you intended." : null)
        );
      }
    }

    if (errors.isEmpty()) {
      SymbolTable.SymbolCollector.collectSymbols(parser.ast);
      errors.addAll(SymbolTable.VerificationPass.verifyUsages(parser.ast));
    }

    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }

    return parser.ast;
  }
}
