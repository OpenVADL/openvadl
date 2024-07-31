package vadl.ast;

import static vadl.ast.AstTestUtils.assertAstEquality;
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

  @Test
  void castPrecedence() {
    var prog = """
        constant a = -4 as Bits<3> + 2
        """;
    var equiv = """
        constant a = ((-4) as Bits<3>) + 2
        """;

    var ast = VadlParser.parse(prog);
    var expected = VadlParser.parse(equiv);
    assertAstEquality(ast, expected);
  }

  @Test
  void chainedCasts() {
    var prog = """
        constant a = -4 as SInt<3> as Bits
        """;
    var equiv = """
        constant a = ((-4) as SInt<3>) as Bits
        """;

    var ast = VadlParser.parse(prog);
    var expected = VadlParser.parse(equiv);
    assertAstEquality(ast, expected);
  }
}
