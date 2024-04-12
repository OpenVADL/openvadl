package vadl.ast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
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
    var scanner = new Scanner(new ByteArrayInputStream(program.getBytes()));
    var parser = new vadl.ast.Parser(scanner);

    // Setting up the Error printing so we can parse it again.
    // This is mainly because coco/r doesn't give us access to the errors internally but always
    // want's to print them.
    var outStream = new ByteArrayOutputStream();
    parser.errors.errorStream = new PrintStream(outStream);
    parser.errors.errMsgFormat = "{0};{1};{2}"; // FIXME: there could be problems with the delimiter

    parser.Parse();
    //var ast = parser.vadl();
    if (parser.errors.count > 0) {
      var lines = outStream.toString().split(System.lineSeparator());
      var errors = Arrays.stream(lines).map(line -> {
        var fields = line.split(";");
        var lineNum = Integer.parseInt(fields[0]);
        var colNum = Integer.parseInt(fields[0]);
        return new VadlError(
            fields[2],
            new Location("unknown.vadl", lineNum, lineNum, colNum, colNum), null, null);
      }).toList();
      //System.out.println("Had " + parser.errors.count + " errors.");
      //System.out.println(outStream.toString());
      //throw new ParserException();
      throw new VadlException(errors);
    }

    return parser.ast;
  }
}
