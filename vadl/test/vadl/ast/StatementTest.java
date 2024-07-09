package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatementTest {

  void verifyPrettifiedAst(Ast ast) {
    var progPretty = ast.prettyPrint();
    var astPretty = Assertions.assertDoesNotThrow(() -> VadlParser.parse(progPretty),
        "Cannot parse prettified input");
    Assertions.assertEquals(ast, astPretty, "Prettified input Ast does not match input Ast");
  }

  @Test
  void parseLetStatement() {
    var prog = """
        instruction set architecture ISA = {
          program counter PC : Bits<32>
          instruction BEQ : Btype = {
            let next = PC + 4 in
              PC := next
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }
}
