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

    // Initialize all bits to "don't care"
    BigInteger mBits = BigInteger.ZERO;
    BigInteger vBits = BigInteger.ZERO;

    // Set fixed bits to their respective encoding value
    for (Encoding.Field encField : insn.encoding().fieldEncodings()) {
      BigInteger fixedValue = encField.constant().integer();

      // Start with the least significant part
      final List<BitSlice.Part> parts = encField.formatField().bitSlice()
          .parts().toList().reversed();

      int offset = 0;
      for (BitSlice.Part p : parts) {
        for (int i = p.lsb(); i <= p.msb(); i++) {
          var val = fixedValue.testBit((offset + i) - p.lsb());
          final int idx = bitIndex(alignedWidth, insnWidth - (i + 1));
          mBits = mBits.setBit(idx);
          if (val) {
            vBits = vBits.setBit(idx);
          }
        }
        offset += p.size();
      }
    }

    if (byteOrder != ByteOrder.LITTLE_ENDIAN || alignedWidth <= 8) {
      // Pattern is already in the correct byte order
      return new BitPattern(mBits, vBits, alignedWidth);
    }

    // Reverse the byte order
    for (int i = 0; i < alignedWidth / 16; i++) {
      for (int j = 0; j < 8; j++) {
        int l = bitIndex(alignedWidth, i * 8 + j);
        int r = bitIndex(alignedWidth, alignedWidth - (i + 1) * 8 + j);

        boolean mL = mBits.testBit(l);
        boolean vL = vBits.testBit(l);

        boolean mR = mBits.testBit(r);
        boolean vR = vBits.testBit(r);

        if (mR) {
          mBits = mBits.setBit(l);
          vBits = vR ? vBits.setBit(l) : vBits.clearBit(l);
        } else {
          mBits = mBits.clearBit(l);
          vBits = vBits.clearBit(l);
        }

        if (mL) {
          mBits = mBits.setBit(r);
          vBits = vL ? vBits.setBit(r) : vBits.clearBit(r);
        } else {
          mBits = mBits.clearBit(r);
          vBits = vBits.clearBit(r);
        }
      }
    }

    return new BitPattern(mBits, vBits, alignedWidth);
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

    // Initialize all bits to "don't care"
    BigInteger mBits = BigInteger.ZERO;
    BigInteger vBits = BigInteger.ZERO;

    // Set fixed bits to their respective encoding value
    BigInteger fixedValue = value.integer();

    // Start with the least significant part
    final List<BitSlice.Part> parts = slice
        .parts().toList().reversed();

    int offset = 0;
    for (BitSlice.Part p : parts) {
      for (int i = p.lsb(); i <= p.msb(); i++) {
        var val = fixedValue.testBit((offset + i) - p.lsb());
        final int idx = bitIndex(alignedWidth, insnWidth - (i + 1));
        mBits = mBits.setBit(idx);
        if (val) {
          vBits = vBits.setBit(idx);
        }
      }
      offset += p.size();
    }

    if (byteOrder != ByteOrder.LITTLE_ENDIAN || alignedWidth <= 8) {
      // Pattern is already in the correct byte order
      return new BitPattern(mBits, vBits, alignedWidth);
    }

    // Reverse the byte order
    for (int i = 0; i < alignedWidth / 16; i++) {
      for (int j = 0; j < 8; j++) {
        int l = bitIndex(alignedWidth, i * 8 + j);
        int r = bitIndex(alignedWidth, alignedWidth - (i + 1) * 8 + j);

        boolean mL = mBits.testBit(l);
        boolean vL = vBits.testBit(l);

        boolean mR = mBits.testBit(r);
        boolean vR = vBits.testBit(r);

        if (mR) {
          mBits = mBits.setBit(l);
          vBits = vR ? vBits.setBit(l) : vBits.clearBit(l);
        } else {
          mBits = mBits.clearBit(l);
          vBits = vBits.clearBit(l);
        }

        if (mL) {
          mBits = mBits.setBit(r);
          vBits = vL ? vBits.setBit(r) : vBits.clearBit(r);
        } else {
          mBits = mBits.clearBit(r);
          vBits = vBits.clearBit(r);
        }
      }
    }

    return new BitPattern(mBits, vBits, alignedWidth);
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

    if (!compatible(p1, p2)) {
      throw new IllegalArgumentException("Patterns have different fixed bits");
    }

    return new BitPattern(
        p1.mask().or(p2.mask()),
        p1.value().or(p2.value()),
        p1.width()
    );
  }

  /**
   * True if two bit patterns (of the same length) do not collide. I.e. there is no bit which is
   * set in both patterns and the values don't match.
   * <br>
   * Another way to look at it could be to say pattern p1 matches pattern p2 (and vice versa).
   *
   * @param p1 The first pattern
   * @param p2 The second pattern
   * @return Whether the patterns are compatible or not.
   */
  public static boolean compatible(BitPattern p1, BitPattern p2) {
    if (p1.width() != p2.width()) {
      return false;
    }
    var sharedMask = p1.mask().and(p2.mask());
    return p1.value().xor(p2.value()).and(sharedMask).signum() == 0;
  }

  /**
   * True if pattern p1 matches a subset of the bit vectors that match pattern p2. I.e. pattern p1
   * is more (or equally) specific. Thus, if a bit vector matches p1 it also matches p2.s
   *
   * @param p1 The first pattern
   * @param p2 The second pattern
   * @return Whether the first pattern is a subset of the second pattern.
   */
  public static boolean contain(BitPattern p1, BitPattern p2) {
    if (p1.width() != p2.width()) {
      return false;
    }

    // Check that any fixed bit in p2 is also a fixed bit in p1.
    if (!p2.mask().and(p1.mask()).equals(p2.mask())) {
      return false;
    }

    // Check that they agree on values of shared mask (mask of p2)
    var valBits1 = p1.value().and(p2.mask());
    var valBits2 = p2.value().and(p2.mask());
    return valBits1.xor(valBits2).signum() == 0;
  }

  /**
   * Replace bits in p with <i>don't care</i> if they are set in the input pattern.
   *
   * @param p            The pattern to remove certain bit from
   * @param inputPattern The pattern specifying the bits to remove.
   * @return A copy of the original pattern p with some bits switched to <i>don't care</i>.
   */
  public static BitPattern invalidate(BitPattern p, BitPattern inputPattern) {
    if (p.width() != inputPattern.width()) {
      throw new IllegalArgumentException("Incompatible pattern widths");
    }

    final var keepMask = widthMask(p.width()).xor(inputPattern.mask());

    return new BitPattern(
        p.mask().and(keepMask),
        p.value().and(keepMask),
        p.width()
    );
  }

  /**
   * Return the common bit pattern of the two argument patterns.
   *
   * @param p1 The first pattern
   * @param p2 The second pattern
   * @return The common super-pattern.
   */
  public static BitPattern commonPattern(BitPattern p1, BitPattern p2) {
    if (p1.width() != p2.width()) {
      throw new IllegalArgumentException(
          "Cannot compute common bits for patterns of different widths");
    }

    // Compute common mask
    var m = p1.mask().and(p2.mask());
    // Remove disagreeing bits from mask
    var diffBits = p1.value().xor(p2.value()).and(m);
    m = m.and(widthMask(p1.width()).xor(diffBits));

    // Compute common value
    var v = p1.value().and(m);
    return new BitPattern(m, v, p1.width());
  }

  private static int bitIndex(int width, int displayIndex) {
    return width - 1 - displayIndex;
  }

  public static BigInteger widthMask(int width) {
    if (width < 0) {
      throw new IllegalArgumentException("Width must be non-negative");
    }
    if (width == 0) {
      return BigInteger.ZERO;
    }
    return BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
  }
}
