package vadl.ast;

import org.junit.jupiter.api.Assertions;

public class AstTestUtils {
  static void verifyPrettifiedAst(Ast ast) {
    var progPretty = ast.prettyPrint();
    var astPretty = Assertions.assertDoesNotThrow(() -> VadlParser.parse(progPretty),
        "Cannot parse prettified input");
    Assertions.assertEquals(ast, astPretty, "Prettified input Ast does not match input Ast");
  }
}
