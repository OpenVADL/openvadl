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
        model addOne(target: Ex) : Ex = {
          1 + $target
        }
        constant two = $addOne(1)
        constant three = $addOne(2)
        constant four = $addOne(1 + 2)
        constant five = $addOne(2 << 1)
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
