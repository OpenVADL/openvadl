package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.VadlException;

public class ExprTest {
  @Test
  void ifExpressions() {
    var prog = """
        constant a = if (5 > 9) then 32 else 2
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void letExpressions() {
    var prog = """
        constant a = let result = 9 in result - 2
        """;
    verifyPrettifiedAst(VadlParser.parse(prog));
  }

  @Test
  void bitLengthCannotContainClosingAngleBracket() {
    var prog = """
        constant a: Bits<31 > 2> = 9
        """;

    Assertions.assertThrows(VadlException.class, () -> VadlParser.parse(prog));
  }
}
