package vadl.ast;

import org.junit.jupiter.api.Assertions;

public class AstTestUtils {
  static void verifyPrettifiedAst(Ast ast) {
    var progPretty = ast.prettyPrint();
    var astPretty = Assertions.assertDoesNotThrow(() -> VadlParser.parse(progPretty),
        "Cannot parse prettified input");
    assertAstEquality(astPretty, ast);
  }

  static void assertAstEquality(Ast actual, Ast expected) {
    if (!actual.equals(expected)) {
      var prettyActual = actual.prettyPrint();
      var prettyExpected = expected.prettyPrint();
      Assertions.assertEquals(actual, expected,
          "Expected: " + prettyExpected + "Actual: " + prettyActual);
    }
  }
}
