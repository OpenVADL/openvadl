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

package vadl.vdt.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.types.BitsType;
import vadl.viam.Constant;
import vadl.viam.Constant.BitSlice;
import vadl.viam.Constant.BitSlice.Part;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

class PatternUtilsTest {

  @Test
  void testContinuousEncoding() {

    /* GIVEN */
    Format f = mockFormat(16, Map.of(
        "f1", new BitSlice(new Part(15, 10)),
        "f2", new BitSlice(new Part(9, 9)),
        "f3", new BitSlice(new Part(8, 0))
    ));

    Instruction i1 = mockInstruction(f, Map.of(
        "f1", new BigInteger("101010", 2),
        "f2", new BigInteger("1", 2),
        "f3", new BigInteger("010101010", 2)
    ));

    Instruction i2 = mockInstruction(f, Map.of(
        "f1", new BigInteger("101010", 2),
        "f3", new BigInteger("010101010", 2)
    ));

    Instruction i3 = mockInstruction(f, Map.of(
        "f3", BigInteger.ZERO
    ));

    /* WHEN */
    BitPattern p1 = PatternUtils.toFixedBitPattern(i1);
    BitPattern p2 = PatternUtils.toFixedBitPattern(i2);
    BitPattern p3 = PatternUtils.toFixedBitPattern(i3);

    /* THEN */
    Assertions.assertEquals("1010101010101010", p1.toString());
    Assertions.assertEquals("101010-010101010", p2.toString());
    Assertions.assertEquals("-------000000000", p3.toString());
  }

  @Test
  void testSplitBitSlices() {

    /* GIVEN */
    Format f = mockFormat(32, Map.of(
        "sf", new BitSlice(new Part(31, 31)),
        "ff", new BitSlice(new Part(29, 29)),
        "op", new BitSlice(new Part(30, 30), new Part(28, 21)),
        "rm", new BitSlice(new Part(20, 16)),
        "option", new BitSlice(new Part(15, 13)),
        "imm3", new BitSlice(new Part(12, 10)),
        "rn", new BitSlice(new Part(9, 5)),
        "rd", new BitSlice(new Part(4, 0))
    ));

    // AAarch64: ADDWUXTB
    Instruction i1 = mockInstruction(f, Map.of(
        "op", new BigInteger("59", 16),
        "sf", BigInteger.ZERO,
        "ff", BigInteger.ZERO,
        "option", BigInteger.ZERO,
        "imm3", BigInteger.ZERO
    ));

    // AAarch64: SUBWUXTB
    Instruction i2 = mockInstruction(f, Map.of(
        "op", new BigInteger("159", 16),
        "sf", BigInteger.ZERO,
        "ff", BigInteger.ZERO,
        "option", BigInteger.ZERO,
        "imm3", BigInteger.ZERO
    ));

    /* WHEN */
    BitPattern p1 = PatternUtils.toFixedBitPattern(i1);
    BitPattern p2 = PatternUtils.toFixedBitPattern(i2);

    /* THEN */
    Assertions.assertEquals("00001011001-----000000----------", p1.toString());
    Assertions.assertEquals("01001011001-----000000----------", p2.toString());
  }

  private Format mockFormat(int size, Map<String, BitSlice> fields) {
    final Format format = new Format(Identifier.noLocation("TestFormat"), BitsType.bits(size));
    final List<Format.Field> f = new ArrayList<>();
    for (var field : fields.entrySet()) {
      Format.Field ff = new Format.Field(Identifier.noLocation(field.getKey()),
          BitsType.bits(field.getValue().bitSize()), field.getValue(), format);
      f.add(ff);
    }
    format.setFields(f.toArray(new Format.Field[0]));
    format.setFieldAccesses(new Format.FieldAccess[0]);
    return format;
  }

  private Instruction mockInstruction(Format format, Map<String, BigInteger> fixed) {

    final List<Encoding.Field> fixedFields = new ArrayList<>();

    for (var f : fixed.entrySet()) {
      Format.Field formatField =
          Arrays.stream(format.fields()).filter(ff -> ff.simpleName().equals(f.getKey()))
              .findFirst().orElse(null);
      Encoding.Field ef = new Encoding.Field(Identifier.noLocation(f.getKey()), formatField,
          Constant.Value.fromInteger(f.getValue(), formatField.type()));
      fixedFields.add(ef);
    }

    Encoding e = new Encoding(Identifier.noLocation("TEST.ENC"), format,
        fixedFields.toArray(new Encoding.Field[0]));
    return new Instruction(Identifier.noLocation(UUID.randomUUID().toString()),
        new Graph("behaviour"), null, e);
  }
}
