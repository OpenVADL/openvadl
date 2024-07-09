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

  @Test
  void parseIfStatement() {
    var prog = """
        instruction set architecture ISA = {
          constant a = 9
          constant b = 2
          program counter PC : Bits<32>
          instruction BEQ : Btype = {
            if a > b then
              PC := PC + 4
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }

  @Test
  void parseIfElseStatement() {
    var prog = """
        instruction set architecture ISA = {
          constant a = 9
          constant b = 2
          program counter PC : Bits<32>
          instruction BEQ : Btype = {
            if a > b then
              PC := PC + 4
            else
              PC := 0
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
    System.out.println(ast.prettyPrint());
  }
}
