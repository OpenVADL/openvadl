package vadl.ast;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InstructionTest {

  @Test
  void parseCombinedInstructionDefinition() {
    var prog = """
        instruction set architecture RV32I = {
          constant hex = 0x12'aDf2
          constant bin = 0b01'010
          register file X : Bits<5> -> Bits<32>
          format R_TYPE : Bits<32> = {
            funct7 [31..25],
            rs2    [24..20],
            rs1    [19..15],
            funct3 [14..12],
            rd     [11..7],
            opcode [6..0]
          }
        }
        """;

    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
  }
}
