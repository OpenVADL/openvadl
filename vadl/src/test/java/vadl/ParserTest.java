package vadl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.ast.Ast;
import vadl.ast.VadlParser;

public class ParserTest {

  void verifyPrettifiedAst(Ast ast) {
    var progPretty = ast.prettyPrint();
    var astPretty = Assertions.assertDoesNotThrow(() -> VadlParser.parse(progPretty),
        "Cannot parse prettified input");
    Assertions.assertEquals(ast, astPretty, "Prettified input Ast does not match input Ast");
  }

  @Test
  void parseEmpty() {
    var prog = "";
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void commonConstantDefinition() {
    var prog = "constant a = 13";
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void emptyIsa() {
    var prog = """
        instruction set architecture imaginaryIsa = {
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void minimalIsa() {
    var prog = """
        instruction set architecture Flo = {
        constant jojo = 42
        constant paul = 40 + 4 * 8
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void comments() {
    var prog = """
        // Some invalid code here 
        /* also here */
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }
}
