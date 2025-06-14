// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.Encoding;
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
  private final LlvmLoweringRecord.Machine llvmLoweringRecord;


  /**
   * Constructor for an instruction in TableGen. It wraps the {@link LlvmLoweringRecord}.
   */
  public TableGenMachineInstruction(String name,
                                    String namespace,
                                    Instruction instruction,
                                    LlvmLoweringRecord.Machine llvmLoweringRecord,
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
    this.fieldEncodings = FieldEncoding.from(instruction.encoding(), inOperands);
    this.instruction = instruction;
    this.llvmLoweringRecord = llvmLoweringRecord;
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

  public LlvmLoweringRecord.Machine llvmLoweringRecord() {
    return llvmLoweringRecord;
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
    /*
    To support multiple encodings with multiple field access functions, we merge the encoded
    fields together. Therefore, the extraction must add the `immediateOffset` since its packed.
     */
    private final int immediateOffset;

    private FieldEncoding(int targetHigh,
                          int targetLow,
                          String sourceBitBlockName,
                          int sourceHigh,
                          int sourceLow,
                          int immediateOffset) {
      this.targetHigh = targetHigh;
      this.targetLow = targetLow;
      this.sourceBitBlockName = sourceBitBlockName;
      this.sourceHigh = sourceHigh;
      this.sourceLow = sourceLow;
      this.immediateOffset = immediateOffset;
    }

    /**
     * Convert an encoding to a TableGen model.
     */
    public static List<FieldEncoding> from(Encoding encoding,
                                           List<TableGenInstructionOperand> inOperands) {
      ArrayList<FieldEncoding> encodings = new ArrayList<>();
      var immediateOffset = 0;
      for (var field : encoding.format().fields()) {
        var sourceOffset = 0;
        var parts = new ArrayList<>(field.bitSlice().parts().toList());
        Collections.reverse(parts);
        boolean isImmediate =
            inOperands.stream()
                .filter(x -> x instanceof ReferencesImmediateOperand)
                .flatMap(x ->
                    ((ReferencesImmediateOperand) x).immediateOperand().fieldAccessRef().fieldRefs()
                        .stream())
                .anyMatch(y -> y == field);
        for (var part : parts) {
          encodings.add(
              new FieldEncoding(part.msb(), part.lsb(), field.simpleName(),
                  sourceOffset + part.size() - 1,
                  sourceOffset,
                  isImmediate ? immediateOffset : 0)); // only field access functions have offset
          sourceOffset += part.size();
        }

        if (isImmediate) {
          immediateOffset += field.size();
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

    public int immediateOffset() {
      return immediateOffset;
    }
  }
}
