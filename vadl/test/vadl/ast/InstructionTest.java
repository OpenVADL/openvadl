package vadl.ast;

import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import org.junit.jupiter.api.Test;

public class InstructionTest {

  @Test
  void parseCombinedInstructionDefinition() {
    var prog = """
        instruction set architecture RV32I = {
          register file X : Bits<5> -> Bits<32>
          format R_TYPE : Bits<32> = {
            funct7 [31..25],
            rs2    [24..20],
            rs1    [19..15],
            funct3 [14..12],
            rd     [11..7],
            opcode [6..0]
          }

          instruction ADD : R_TYPE = {
            X(rd) := X(rs1) + X(rs2)
          }

          encoding ADD = {
            opcode = 0b011'0011,
            funct3 = 0b000,
            funct7 = 0b000'0000
          }

          assembly ADD = (mnemonic, " ", rd, ", ", rs1, ", ", rs2)
        }
        """;

    var ast = VadlParser.parse(prog);
    verifyPrettifiedAst(ast);
  }
}
