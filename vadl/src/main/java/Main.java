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
    var program = "1+2 / 4\n";

    try {
      var ast = VadlParser.parse(program);
      System.out.println(ast.dump());
      System.out.println(ast.prettyPrint());
    } catch (VadlException e) {
      var printer = new ErrorPrinter();
      printer.print(e.errors);
      System.exit(1);
    }
  }
}
