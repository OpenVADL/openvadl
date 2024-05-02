package vadl.ast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import vadl.error.VadlError;
import vadl.error.VadlException;

/**
 * A parser for the VADL language, generated using Coco.
 */
public class VadlParser {

  /**
   * Parses a source program into an AST.
   *
   * @param program a source code file to parse
   * @return The parsed syntax tree.
   * @throws VadlException if there are any parsing errors.
   */
  public static Ast parse(String program) {
    var scanner = new Scanner(new ByteArrayInputStream(program.getBytes(StandardCharsets.UTF_8)));
    var parser = new vadl.ast.Parser(scanner);

    // Setting up the Error printing so we can parse it again.
    // This is mainly because coco/r doesn't give us access to the errors internally but always
    // want's to print them.
    var outStream = new ByteArrayOutputStream();
    parser.errors.errorStream = new PrintStream(outStream);
    parser.errors.errMsgFormat = "{0};{1};{2}"; // FIXME: there could be problems with the delimiter

    parser.Parse();

    List<VadlError> errors = new ArrayList<>();
    if (parser.errors.count > 0) {
      var lines = outStream.toString(StandardCharsets.UTF_8).split(System.lineSeparator(), -1);
      for (var line : lines) {
        if (line.trim().isEmpty()) {
          continue;
        }

        System.out.println(line);
        var fields = line.split(";", -1);
        System.out.println(Arrays.toString(fields));
        var lineNum = Integer.parseInt(fields[0]);
        var colNum = Integer.parseInt(fields[1]);
        errors.add(new VadlError(
            fields[2],
            new Location("unknown.vadl", lineNum, lineNum, colNum, colNum), null, null)
        );
      }
      ;
    }

    try {
      parser.symbolTable.verifyAllUsages();
    } catch (VadlException e) {
      errors.addAll(e.errors);
    }

    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }

    return parser.ast;
  }
}
