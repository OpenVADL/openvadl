package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatementTest {

  @Test
  void parseLetStatement() {
    var prog = """
        instruction set architecture ISA = {
          program counter PC : Bits<32>
          format Btype : Bits<32> = {
            bits [31..0]
          }
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
          format Btype : Bits<32> = {
            bits [31..0]
          }
          instruction BEQ : Btype = {
            if a > b then
              PC := PC + 4
          }
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void parseIfElseStatement() {
    var prog = """
        instruction set architecture ISA = {
          constant a = 9
          constant b = 2
          program counter PC : Bits<32>
          format Btype : Bits<32> = {
            bits [31..0]
          }
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
  }
}
