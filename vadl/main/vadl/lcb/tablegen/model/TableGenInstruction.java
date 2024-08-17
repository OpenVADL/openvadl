package vadl.lcb.tablegen.model;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.viam.Instruction;
import vadl.viam.Register;

/**
 * Models an {@link Instruction} for TableGen.
 */
public class TableGenInstruction extends TableGenRecord {
  private final String name;
  private final String namespace;
  private final List<TableGenPattern> pattern;
  private final List<TableGenInstructionOperand> inOperands;
  private final List<TableGenInstructionOperand> outOperands;
  private final List<Register> uses;
  private final List<Register> defs;
  private final int formatSize;
  private final int size;
  private final int codeSize;
  private final Flags flags;
  private final List<BitBlock> bitBlocks;
  private final List<FieldEncoding> fieldEncodings;

  public TableGenInstruction(String name,
                             String namespace,
                             Instruction instruction,
                             Flags flags,
                             List<TableGenInstructionOperand> inOperands,
                             List<TableGenInstructionOperand> outOperands,
                             List<Register> uses,
                             List<Register> defs) {
    this(name, namespace, instruction, flags, inOperands, outOperands, uses, defs,
        Collections.emptyList());
  }

  /**
   * Constructor for an instruction in TableGen.
   */
  public TableGenInstruction(String name,
                             String namespace,
                             Instruction instruction,
                             Flags flags,
                             List<TableGenInstructionOperand> inOperands,
                             List<TableGenInstructionOperand> outOperands,
                             List<Register> uses,
                             List<Register> defs,
                             List<TableGenPattern> pattern) {
    this.name = name;
    this.namespace = namespace;
    this.formatSize = instruction.encoding().format().type().bitWidth();
    this.size = instruction.encoding().format().type().bitWidth() / 8;
    this.codeSize = instruction.encoding().format().type().bitWidth() / 8;
    this.bitBlocks = BitBlock.from(instruction.encoding());
    this.fieldEncodings = FieldEncoding.from(instruction.encoding());
    this.flags = flags;
    this.inOperands = inOperands;
    this.outOperands = outOperands;
    this.uses = uses;
    this.defs = defs;
    this.pattern = pattern;
  }

  public String getNamespace() {
    return namespace;
  }

  public List<TableGenPattern> getPattern() {
    return pattern;
  }

  public List<Register> getUses() {
    return uses;
  }

  public List<Register> getDefs() {
    return defs;
  }

  public int getSize() {
    return size;
  }

  public int getCodeSize() {
    return codeSize;
  }

  public Flags getFlags() {
    return flags;
  }

  public List<BitBlock> getBitBlocks() {
    return bitBlocks;
  }

  public List<FieldEncoding> getFieldEncodings() {
    return fieldEncodings;
  }

  public List<TableGenInstructionOperand> getInOperands() {
    return inOperands;
  }

  public List<TableGenInstructionOperand> getOutOperands() {
    return outOperands;
  }

  public String getName() {
    return name;
  }

  public int getFormatSize() {
    return formatSize;
  }

  /**
   * A {@link TableGenInstruction} has many boolean flags which are required for the
   * code generation.
   */
  public record Flags(boolean isTerminator,
                      boolean isBranch,
                      boolean isCall,
                      boolean isReturn,
                      boolean isPseudo,
                      boolean isCodeGenOnly,
                      boolean mayLoad,
                      boolean mayStore) {

  }

  /**
   * A machine instruction has certain encoding parts which are fixed like the opcode.
   * A {@link BitBlock} represents constant bits in an instruction.
   */
  public static class BitBlock {
    private final int size;
    private final String name;
    private final Optional<BitSet> bitSet;

    private BitBlock(int size, String name, Optional<BitSet> bitSet) {
      this.size = size;
      this.name = name;
      this.bitSet = bitSet;
    }

    /**
     * Convert an encoding into a bitblock set for TableGen.
     */
    public static List<BitBlock> from(vadl.viam.Encoding encoding) {
      var encodedFields = Arrays.stream(encoding.fieldEncodings())
          .map(field -> new BitBlock(field.formatField().size(), field.name(),
              Optional.of(BitSet.valueOf(new long[] {field.constant().longValue()}))));
      var nonEncodedFields = Arrays.stream(encoding.nonEncodedFormatFields())
          .map(field -> new BitBlock(field.size(), field.name(), Optional.empty()));

      return Stream.concat(encodedFields, nonEncodedFields).toList();
    }

    public int getSize() {
      return size;
    }

    public String getName() {
      return name;
    }

    public Optional<BitSet> getBitSet() {
      return bitSet;
    }
  }

  /**
   * It defines the mapping from a {@link BitBlock} to a {@link TableGenInstruction}.
   */
  public static class FieldEncoding {
    private final int targetHigh;
    private final int targetLow;
    private final String sourceBitBlockName;
    private final int sourceHigh;
    private final int sourceLow;

    private FieldEncoding(int targetHigh, int targetLow, String sourceBitBlockName, int sourceHigh,
                          int sourceLow) {
      this.targetHigh = targetHigh;
      this.targetLow = targetLow;
      this.sourceBitBlockName = sourceBitBlockName;
      this.sourceHigh = sourceHigh;
      this.sourceLow = sourceLow;
    }

    /**
     * Convert an encoding to a TableGen model.
     */
    public static List<FieldEncoding> from(vadl.viam.Encoding encoding) {
      return Arrays.stream(encoding.format().fields()).map(field -> {
        field.bitSlice().ensure(field.bitSlice().isContinuous(), "bitSlice must be continuous");
        return new FieldEncoding(field.bitSlice().msb(), field.bitSlice().lsb(), field.name(),
            field.size() - 1, 0);
      }).toList();
    }

    public int getTargetHigh() {
      return targetHigh;
    }

    public int getTargetLow() {
      return targetLow;
    }

    public String getSourceBitBlockName() {
      return sourceBitBlockName;
    }

    public int getSourceHigh() {
      return sourceHigh;
    }

    public int getSourceLow() {
      return sourceLow;
    }
  }
}
