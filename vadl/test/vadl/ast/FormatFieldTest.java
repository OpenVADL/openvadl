package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FormatFieldTest {
  @Test
  void slicedFormat() {
    var prog = """
        format I_TYPE : Bits<32> =
        { funct6 [31..26]
        , shamt  [25..20]
        , rs1    [19..15]
        , funct3 [14..12]
        , rd     [11..7]
        , opcode [6..0]
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void typedFormat() {
    var prog = """
        format I_TYPE : Bits<32> =
        { funct6 : Bits<6>
        , shamt  : Bits<6>
        , rs1    : Bits<5>
        , funct3 : Bits<3>
        , rd     : Bits<5>
        , opcode : Bits<7>
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void formatReferencesAreAllowed() {
    var prog = """
        format BYTE : Bits<8> = { bits [7..0] }
        format WORD : Bits<16> = { high: BYTE, low: BYTE }
        format ADDRESS : Bits<64> =
        { a: WORD
        , b: WORD
        , c: WORD
        , d: WORD
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }
}
