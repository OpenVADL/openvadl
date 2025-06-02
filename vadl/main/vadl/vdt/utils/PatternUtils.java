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

import static vadl.vdt.utils.PBit.Value.DONT_CARE;
import static vadl.vdt.utils.PBit.Value.ONE;
import static vadl.vdt.utils.PBit.Value.ZERO;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;
import vadl.viam.Constant;
import vadl.viam.Constant.BitSlice;
import vadl.viam.Encoding;
import vadl.viam.Format;

/**
 * Utility methods to construct bit patterns used for VDT generation.
 */
public class PatternUtils {

  private PatternUtils() {
    // Static utility class
  }

  /**
   * Returns the bit pattern, where fixed bits in the instruction encoding are set to their
   * respective encoding value. All other bits are set to <i>don't care</i>.
   * <br>
   * The patterns will be constructed as the instructions appear in memory, i.e. in accordance with
   * the architecture's endianness. For instructions which are not a byte-multiple, the pattern
   * will be padded with <i>don't care</i> bits accordingly.
   *
   * @param insn      The instruction
   * @param byteOrder The architecture's byte order
   * @return The bit pattern
   */
  public static BitPattern toFixedBitPattern(vadl.viam.Instruction insn, ByteOrder byteOrder) {

    // Instruction definitions are in natural order (big endian), i.e. with the most significant
    // byte first.
    final int insnWidth = insn.format().type().bitWidth();
    final int alignedWidth = insnWidth % 8 != 0 ? insnWidth + (8 - insnWidth % 8) : insnWidth;
    final PBit[] bits = new PBit[alignedWidth];

    // Initialize all bits to "don't care"
    for (int i = 0; i < bits.length; i++) {
      bits[i] = new PBit(PBit.Value.DONT_CARE);
    }

    // Set fixed bits to their respective encoding value
    for (Encoding.Field encField : insn.encoding().fieldEncodings()) {
      BigInteger fixedValue = encField.constant().integer();

      // Start with the least significant part
      final List<BitSlice.Part> parts = encField.formatField().bitSlice()
          .parts().toList().reversed();

      int offset = 0;
      for (BitSlice.Part p : parts) {
        for (int i = p.lsb(); i <= p.msb(); i++) {
          var val = fixedValue.testBit((offset + i) - p.lsb()) ? ONE : ZERO;
          bits[insnWidth - (i + 1)] = new PBit(val);
        }
        offset += p.size();
      }
    }

    if (byteOrder != ByteOrder.LITTLE_ENDIAN || alignedWidth <= 8) {
      // Pattern is already in the correct byte order
      return new BitPattern(bits);
    }

    // Reverse the byte order
    for (int i = 0; i < alignedWidth / 16; i++) {
      for (int j = 0; j < 8; j++) {
        int l = i * 8 + j;
        int r = alignedWidth - (i + 1) * 8 + j;
        PBit tmp = bits[l];
        bits[l] = bits[r];
        bits[r] = tmp;
      }
    }

    return new BitPattern(bits);
  }

  /**
   * Returns the bit pattern for the given encoding field, where all bits not encoded by this field
   * are set to <i>don't care</i>.
   *
   * @param field     The encoded field
   * @param value     The encoded value
   * @param byteOrder The architecture's byte order
   * @return The bit pattern
   */
  public static BitPattern toFixedBitPattern(Format.Field field, Constant.Value value,
                                             ByteOrder byteOrder) {
    return toFixedBitPattern(field.format(), field.bitSlice(), value, byteOrder);
  }

  /**
   * Returns the bit pattern for the given encoding field, where all bits not encoded by this field
   * are set to <i>don't care</i>.
   *
   * @param format    The instruction format
   * @param slice     The bit slice of the field to encode
   * @param value     The encoded value
   * @param byteOrder The architecture's byte order
   * @return The bit pattern
   */
  public static BitPattern toFixedBitPattern(Format format, BitSlice slice, Constant.Value value,
                                             ByteOrder byteOrder) {

    // Instruction definitions are in natural order (big endian), i.e. with the most significant
    // byte first.
    final int insnWidth = format.type().bitWidth();
    final int alignedWidth = insnWidth % 8 != 0 ? insnWidth + (8 - insnWidth % 8) : insnWidth;
    final PBit[] bits = new PBit[alignedWidth];

    // Initialize all bits to "don't care"
    for (int i = 0; i < bits.length; i++) {
      bits[i] = new PBit(PBit.Value.DONT_CARE);
    }

    // Set fixed bits to their respective encoding value
    BigInteger fixedValue = value.integer();

    // Start with the least significant part
    final List<BitSlice.Part> parts = slice
        .parts().toList().reversed();

    int offset = 0;
    for (BitSlice.Part p : parts) {
      for (int i = p.lsb(); i <= p.msb(); i++) {
        var val = fixedValue.testBit((offset + i) - p.lsb()) ? ONE : ZERO;
        bits[insnWidth - (i + 1)] = new PBit(val);
      }
      offset += p.size();
    }

    if (byteOrder != ByteOrder.LITTLE_ENDIAN || alignedWidth <= 8) {
      // Pattern is already in the correct byte order
      return new BitPattern(bits);
    }

    // Reverse the byte order
    for (int i = 0; i < alignedWidth / 16; i++) {
      for (int j = 0; j < 8; j++) {
        int l = i * 8 + j;
        int r = alignedWidth - (i + 1) * 8 + j;
        PBit tmp = bits[l];
        bits[l] = bits[r];
        bits[r] = tmp;
      }
    }

    return new BitPattern(bits);
  }

  /**
   * Combine the two bit-patterns (of the same size) to one. If the patterns have different fixed
   * bits at the same position, this throws an exception.
   *
   * @param p1 The first pattern
   * @param p2 The second pattern
   * @return The combined pattern.
   */
  public static BitPattern combinePatterns(BitPattern p1, BitPattern p2) {
    if (p1.width() != p2.width()) {
      throw new IllegalArgumentException("Patterns of different widths cannot be combined");
    }
    final PBit[] bits = new PBit[p1.width()];
    for (int i = 0; i < bits.length; i++) {
      final PBit.Value v1 = p1.get(i).getValue();
      final PBit.Value v2 = p2.get(i).getValue();
      if (v1 != v2 && v1 != DONT_CARE && v2 != DONT_CARE) {
        throw new IllegalArgumentException("Patterns have different fixed bits at position " + i);
      }
      if (v1 != DONT_CARE) {
        bits[i] = new PBit(v1);
      } else {
        bits[i] = new PBit(v2);
      }
    }
    return new BitPattern(bits);
  }
}
