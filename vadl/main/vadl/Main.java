package vadl;

import vadl.ast.VadlParser;
import vadl.error.ErrorPrinter;
import vadl.error.VadlException;

/**
 * The main vadl program, providing a cli.
 */
public class Main {

  /**
   * The main vadl method, providing a cli.
   *
   * @param args are teh command line arguments.
   */
  public static void main(String[] args) {
    var program = """
        format I_TYPE : Bits<32> =
        { funct6 [31..26]
        , shamt  [25..20]
        , rs1    [19..15]
        , funct3 [14..12]
        , rd     [11..7]
        , opcode [6..0]
        }
        """;

    try {
      var ast = VadlParser.parse(program);
      System.out.println("== AST DUMP ==");
      System.out.println(ast.dump());
      System.out.println("== AST PRETTY PRINT ==");
      System.out.println(ast.prettyPrint());
    } catch (VadlException e) {
      var printer = new ErrorPrinter();
      printer.print(e.errors);
      System.exit(1);
    }
  }
}
