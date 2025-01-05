package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Instruction;

/**
 * Represents a record in tablegen for {@link Instruction}.
 */
public class TableGenMachineInstruction extends TableGenInstruction {
  private final int formatSize;
  private final int size;
  private final int codeSize;
  private final List<BitBlock> bitBlocks;
  private final List<FieldEncoding> fieldEncodings;
  private final Instruction instruction;


  /**
   * Constructor for an instruction in TableGen.
   */
  public TableGenMachineInstruction(String name,
                                    String namespace,
                                    Instruction instruction,
                                    LlvmLoweringPass.Flags flags,
                                    List<TableGenInstructionOperand> inOperands,
                                    List<TableGenInstructionOperand> outOperands,
                                    List<RegisterRef> uses,
                                    List<RegisterRef> defs,
                                    List<TableGenPattern> anonymousPatterns) {
    super(name, namespace, flags, inOperands, outOperands, uses, defs, anonymousPatterns);
    this.formatSize = instruction.encoding().format().type().bitWidth();
    this.size = instruction.encoding().format().type().bitWidth() / 8;
    this.codeSize = instruction.encoding().format().type().bitWidth() / 8;
    this.bitBlocks = BitBlock.from(instruction.encoding());
    this.fieldEncodings = FieldEncoding.from(instruction.encoding());
    this.instruction = instruction;
  }


  public int getSize() {
    return size;
  }

  public int getCodeSize() {
    return codeSize;
  }

  public List<BitBlock> getBitBlocks() {
    return bitBlocks;
  }

  public List<FieldEncoding> getFieldEncodings() {
    return fieldEncodings;
  }

  public int getFormatSize() {
    return formatSize;
  }

  public Instruction instruction() {
    return instruction;
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
          .map(field -> new BitBlock(field.formatField().size(), field.simpleName(),
              Optional.of(BitSet.valueOf(new long[] {field.constant().longValue()}))));
      var nonEncodedFields = Arrays.stream(encoding.nonEncodedFormatFields())
          .map(field -> new BitBlock(field.size(), field.simpleName(), Optional.empty()));

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
      ArrayList<FieldEncoding> encodings = new ArrayList<>();
      for (var field : encoding.format().fields()) {
        var offset = 0;
        var parts = new ArrayList<>(field.bitSlice().parts().toList());
        Collections.reverse(parts);
        for (var part : parts) {
          encodings.add(
              new FieldEncoding(part.msb(), part.lsb(), field.simpleName(),
                  offset + part.size() - 1,
                  offset));
          offset += part.size();
        }
      }
      return encodings;
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
