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

package vadl.iss;

import static vadl.utils.MemOrderUtils.reverseByteOrder;

import com.google.errorprone.annotations.FormatMethod;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import vadl.TestUtils;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.utils.Disassembler;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.FieldRefNode;

public class AutoAssembler {

  final Disassembler disassembler;
  private List<Integer> allowedRegIndices;
  private final ByteOrder byteOrder;

  public AutoAssembler(Disassembler disassembler, ByteOrder byteOrder) {
    this.disassembler = disassembler;
    this.byteOrder = byteOrder;
  }

  public AutoAssembler allowRegisterIndices(int startInclusive, int endExclusive) {
    allowRegisterIndices(IntStream.range(startInclusive, endExclusive).boxed().toList());
    return this;
  }

  public AutoAssembler allowRegisterIndices(List<Integer> allowedRegIndices) {
    this.allowedRegIndices = allowedRegIndices;
    return this;
  }

  public Result produce(Instruction instruction) {
    checkState();

    // Field : Encoding Value
    var assignment = new HashMap<Format.Field, BigInteger>();

    var enc = instruction.encoding();
    for (var f : enc.fieldEncodings()) {
      assignment.put(f.formatField(), f.constant().integer());
    }
    
    var regs = regIndexFields(instruction);
    var destRegs = new ArrayList<Format.Field>();
    var srcRegs = new ArrayList<Format.Field>();
    for (var reg : regs.entrySet()) {
      var field = reg.getValue();
      var tcgV = reg.getKey();
      // select register indices
      var i = Arbitraries.of(allowedRegIndices).sample();
      assignment.put(field, BigInteger.valueOf(i));
      if (tcgV.isDest()) {
        destRegs.add(field);
      } else {
        srcRegs.add(field);
      }
    }

    for (var f : enc.nonEncodedFormatFields()) {
      assignment.computeIfAbsent(f, k -> TestUtils.arbitraryBits(k.size()).sample());
    }

    var instrCode = encode(assignment);
    var assembly = disassembler.disassemble(instrCode);
    return new Result(srcRegs, destRegs, assignment, assembly, instrCode);
  }

  private void checkState() {
    check(!allowedRegIndices.isEmpty(), "There must be at least one allowed register to be used.");
  }

  private Map<TcgV, Format.Field> regIndexFields(Instruction instruction) {
    var fields = new HashMap<TcgV, Format.Field>();
    for (var fieldRef : instruction.behavior().getNodes(FieldRefNode.class).toList()) {
      var regVar = regVar(fieldRef);
      if (regVar != null) {
        fields.put(regVar, fieldRef.formatField());
      }
    }
    return fields;
  }

  @Nullable
  private TcgV regVar(FieldRefNode fieldRef) {
    return fieldRef.usages()
        .filter(n -> n instanceof TcgVRefNode)
        .map(n -> ((TcgVRefNode) n).var())
        .filter(n -> n.kind() == TcgV.Kind.REG_TENSOR)
        .findFirst().orElse(null);
  }


  private Constant.Value encode(Map<Format.Field, BigInteger> fields) {
    var format = fields.entrySet().stream().findFirst().get().getKey().format();
    if (format.fields().length != fields.size()) {
      throw new IllegalArgumentException("Wrong number of fields");
    }

    var encoding = BigInteger.ZERO;
    for (var entry : fields.entrySet()) {
      // encode each entry into the final encoding
      var field = entry.getKey();
      var value = entry.getValue();
      // the positions in the encoding value for each bit in the value
      var destPos = field.bitSlice().stream().boxed().toArray(Integer[]::new);
      for (int i = 0; i < field.size(); i++) {
        // set each bit in the value
        var pos = field.size() - i - 1;
        if (value.testBit(pos)) {
          // if the bit is set in the value, we set it in the encoding
          encoding = encoding.setBit(destPos[i]);
        }
      }
    }

    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
      encoding = reverseByteOrder(encoding, format.type().bitWidth());
    }
    return Constant.Value.fromInteger(encoding, format.type());
  }


  @FormatMethod
  private void check(boolean condition, String fmt, Object... args) {
    if (!condition) {
      throw new IllegalArgumentException(String.format(fmt, args));
    }
  }

  public record Result(
      List<Format.Field> srcRegs,
      List<Format.Field> destRegs,
      Map<Format.Field, BigInteger> assignment,
      String assembly,
      Constant.Value instrCode
  ) {

  }
}
