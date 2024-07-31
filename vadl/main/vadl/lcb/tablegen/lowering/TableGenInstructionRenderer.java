package vadl.lcb.tablegen.lowering;

import vadl.lcb.tablegen.model.TableGenInstruction;

/**
 * Utility class for mapping into tablegen.
 */
public final class TableGenInstructionRenderer {
  /**
   * Transforms the given {@code instruction} into a string which can be used by LLVM's TableGen.
   */
  public static String lower(TableGenInstruction instruction) {
    return String.format("""
            def ADD : Instruction
            {
                 let Namespace = "%s";
             
                 let Size = %d; // Bits<32>
                 let CodeSize = %d; // Bits<32>, used for ISEL cost
             
                 let OutOperandList = ( outs %s );
                 let InOperandList = ( ins %s );
             
                 field bits<32> Inst;
                \s
                 // SoftFail is a field the disassembler can use to provide a way for
                 // instructions to not match without killing the whole decode process. It is
                 // mainly used for ARM, but Tablegen expects this field to exist or it fails
                 // to build the decode table.
                 field bits<32> SoftFail = 0;
                \s
                 bits<3> funct3 = 0b000;
                 bits<7> funct7 = 0b0000000;
                 bits<7> opcode = 0b0110011;
                 bits<5> rs2;
                 bits<5> rs1;
                 bits<5> rd;
             
                 let Inst{14-12} = funct3{2-0};
                 let Inst{31-25} = funct7{6-0};
                 let Inst{6-0} = opcode{6-0};
                 let Inst{24-20} = rs2{4-0};
                 let Inst{19-15} = rs1{4-0};
                 let Inst{11-7} = rd{4-0};
             
                 let isTerminator  = %d;
                 let isBranch      = %d;
                 let isCall        = %d;
                 let isReturn      = %d;
                 let isPseudo      = %d;
                 let isCodeGenOnly = %d;
                 let mayLoad       = %d;
                 let mayStore      = %d;
             
                 let Constraints = "";
                 let AddedComplexity = 0;
             
                 let Pattern = [];
             
                 let Uses = [ ];
                 let Defs = [ ];
            }
            """,
        instruction.getNamespace(),
        instruction.getSize(),
        instruction.getCodeSize(),
        "out",
        "in",
        toInt(instruction.isTerminator()),
        toInt(instruction.isBranch()),
        toInt(instruction.isCall()),
        toInt(instruction.isReturn()),
        toInt(instruction.isPseudo()),
        toInt(instruction.isCodeGenOnly()),
        toInt(instruction.isMayLoad()),
        toInt(instruction.isMayStore())
    );
  }

  private static int toInt(boolean b) {
    return b ? 1 : 0;
  }
}
