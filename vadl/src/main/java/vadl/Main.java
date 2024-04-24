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
        
        """;

    try {
      var ast = VadlParser.parse(program);
      System.out.println("== AST DUMP ==");
      System.out.println(ast.dump());
      System.out.println("== AST PRETTY PRINT ==");
      System.out.println(ast.prettyPrint());

      var p2 = ast.prettyPrint();
      System.out.printf("'%s'\n", p2);
      var a2 = VadlParser.parse(p2);
      System.out.println(a2.equals(ast));
    } catch (VadlException e) {
      var printer = new ErrorPrinter();
      printer.print(e.errors);
      System.exit(1);
    }
  }
}
