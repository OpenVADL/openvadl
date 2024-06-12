package vadl;

import vadl.ast.AstDumper;
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
        /**
        * NOTES for first macros:
        * - Multiple macros
        * - Multiple invocations
        * - Macro in macro
        * - Correct binding in invocation
        * - Correct reorder in macro
        */
                
        model first() : Ex = {
          1
        }
                
        model second() : Ex = {
          1 + $first()
        }
                
        constant a = $first()
        constant b = $second()
        """;

    try {
      var ast = VadlParser.parse(program);
      System.out.println("== AST DUMP ==");
      var dumper = new AstDumper();
      System.out.println(dumper.dump(ast));
      System.out.println("== AST PRETTY PRINT ==");
      System.out.println(ast.prettyPrint());
    } catch (VadlException e) {
      var printer = new ErrorPrinter();
      printer.print(e.errors);
      System.exit(1);
    }
  }
}
