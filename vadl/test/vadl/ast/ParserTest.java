package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A class to test the parser. Since the parser does  also name resolution and macro expansion this
 * class only focuses on correct parsing and the two other tasks are tested in different classes.
 */
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

  @Test
  void contantsWithTypeAnnotation() {
    var prog = """
        constant a: Bool = 1
        constant b: Bits<12> = 12
        constant c: SInt<64> = 42
        constant d: UInt<32> = 13
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void contantsWithAdvancedTypeAnnotation() {
    // FIXME: we will need to adapt this test once we check if certain types exist
    var prog = """
        constant size = 64
                
        constant a: customBoolean = 1
        constant b: Bits<size> = 1
        constant c: SInt<1+2> = 1
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void memoryDefinition() {
    var prog = """
        instruction set architecture FLO = {
          memory mem: Bits<32> -> Bits<8>
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }
}
