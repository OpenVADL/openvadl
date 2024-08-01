package vadl.lcb.tablegen.lowering;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import vadl.lcb.tablegen.model.TableGenInstruction;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.viam.Definition;

/**
 * Utility class for mapping into tablegen.
 */
public final class TableGenInstructionRenderer {
  /**
   * Transforms the given {@code instruction} into a string which can be used by LLVM's TableGen.
   */
  public static String lower(TableGenInstruction instruction) {
    return String.format("""
            def %s : Instruction
            {
                 let Namespace = "%s";
             
                 let Size = %d;
                 let CodeSize = %d;
             
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
                 %s
             
                 %s
             
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
             
                 let Uses = [ %s ];
                 let Defs = [ %s ];
            }
            """,
        instruction.getName(),
        instruction.getNamespace(),
        instruction.getSize(),
        instruction.getCodeSize(),
        instruction.getOutOperands().stream().map(TableGenInstructionRenderer::lower),
        instruction.getInOperands().stream().map(TableGenInstructionRenderer::lower),
        instruction.getBitBlocks().stream().map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining("\n")),
        instruction.getFieldEncodings().stream().map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining("\n")),
        toInt(instruction.getFlags().isTerminator()),
        toInt(instruction.getFlags().isBranch()),
        toInt(instruction.getFlags().isCall()),
        toInt(instruction.getFlags().isReturn()),
        toInt(instruction.getFlags().isPseudo()),
        toInt(instruction.getFlags().isCodeGenOnly()),
        toInt(instruction.getFlags().mayLoad()),
        toInt(instruction.getFlags().mayStore()),
        instruction.getUses().stream().map(Definition::name).collect(Collectors.joining(",")),
        instruction.getDefs().stream().map(Definition::name).collect(Collectors.joining(","))
    );
  }

  private static String lower(TableGenInstructionOperand operand) {
    return String.format("""
        %s:$%s
        """, operand.type(), operand.name());
  }

  private static String lower(TableGenInstruction.BitBlock bitBlock) {
    if (bitBlock.getBitSet().isPresent()) {
      return String.format("bits<%s> %s = 0b%s", bitBlock.getSize(), bitBlock.getName(),
          toBinaryString(bitBlock.getBitSet().get()));
    } else {
      return String.format("bits<%s> %s;", bitBlock.getSize(), bitBlock.getName());
    }
  }

  private static String lower(TableGenInstruction.FieldEncoding fieldEncoding) {
    return String.format("""
            let Inst{%s-%s} = %s{%s-%s};
            """, fieldEncoding.getTargetHigh(),
        fieldEncoding.getTargetLow(),
        fieldEncoding.getSourceBitBlockName(),
        fieldEncoding.getSourceHigh(),
        fieldEncoding.getSourceLow());
  }

  /**
   * @param bitSet bitset
   * @return "01010000" binary string
   */
  private static String toBinaryString(BitSet bitSet) {
    if (bitSet == null) {
      return null;
    }
    return IntStream.range(0, bitSet.length())
        .mapToObj(b -> String.valueOf(bitSet.get(b) ? 1 : 0))
        .collect(Collectors.joining());
  }

  private static int toInt(boolean b) {
    return b ? 1 : 0;
  }
}
