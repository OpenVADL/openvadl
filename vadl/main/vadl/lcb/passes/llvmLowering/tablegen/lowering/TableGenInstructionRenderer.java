package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
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

            field bits<%s> Inst;

            // SoftFail is a field the disassembler can use to provide a way for
            // instructions to not match without killing the whole decode process. It is
            // mainly used for ARM, but Tablegen expects this field to exist or it fails
            // to build the decode table.
            field bits<%s> SoftFail = 0;
                             
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

            %s
            """,
        instruction.getName(),
        instruction.getNamespace(),
        instruction.getSize(),
        instruction.getCodeSize(),
        instruction.getOutOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        instruction.getInOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        instruction.getFormatSize(),
        instruction.getFormatSize(),
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
        instruction.getDefs().stream().map(Definition::name).collect(Collectors.joining(",")),
        instruction.getAnonymousPatterns().stream().map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining("\n"))
    );
  }

  private static String lower(TableGenPattern tableGenPattern) {
    var visitor = new TableGenPatternPrinterVisitor();
    var machineVisitor = new TableGenMachineInstructionPrinterVisitor();

    for (var root : tableGenPattern.selector().getDataflowRoots()) {
      visitor.visit(root);
    }

    for (var root : tableGenPattern.machine().getDataflowRoots()) {
      machineVisitor.visit((MachineInstructionNode) root);
    }

    return String.format("""
        def : Pat<%s
                %s>;
          """, visitor.getResult(), machineVisitor.getResult());
  }

  private static String lower(TableGenInstructionOperand operand) {
    return operand.identity().render();
  }

  private static String lower(TableGenInstruction.BitBlock bitBlock) {
    if (bitBlock.getBitSet().isPresent()) {
      return String.format("bits<%s> %s = 0b%s;", bitBlock.getSize(), bitBlock.getName(),
          toBinaryString(bitBlock.getBitSet().get()));
    } else {
      return String.format("bits<%s> %s;", bitBlock.getSize(), bitBlock.getName());
    }
  }

  private static String lower(TableGenInstruction.FieldEncoding fieldEncoding) {
    return String.format("let Inst{%s-%s} = %s{%s-%s};", fieldEncoding.getTargetHigh(),
        fieldEncoding.getTargetLow(),
        fieldEncoding.getSourceBitBlockName(),
        fieldEncoding.getSourceHigh(),
        fieldEncoding.getSourceLow());
  }

  /**
   * Converts a bitset into string representation.
   *
   * @param bitSet bitset
   * @return "01010000" binary string
   */
  @Nullable
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
