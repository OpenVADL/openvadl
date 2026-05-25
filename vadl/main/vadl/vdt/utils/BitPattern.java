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

import static vadl.vdt.utils.PatternUtils.widthMask;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Represents a bit pattern, which is a vector of bits where each bit can be either 0, 1 or <i>don't
 * care</i>.
 */
public class BitPattern {

  private final BigInteger mask;
  private final BigInteger value;
  private final int width;

  /**
   * Represents the state of a bit in a bit pattern.
   */
  public enum PatternBit {
    ZERO, ONE, DONT_CARE
  }

  /**
   * Creates a new bit pattern.
   *
   * @param mask  the mask specifying the bits to consider
   * @param value the value of the bits
   * @param width the width of the bit pattern
   */
  public BitPattern(BigInteger mask, BigInteger value, int width) {
    if (width < 0) {
      throw new IllegalArgumentException("Width must be non-negative");
    }
    this.mask = Objects.requireNonNull(mask).and(widthMask(width));
    this.value = Objects.requireNonNull(value).and(this.mask);
    this.width = width;
  }

  public BigInteger mask() {
    return mask;
  }

  public BigInteger value() {
    return value;
  }

  public int width() {
    return width;
  }

  /**
   * Returns the bit at the given index with MSB-first semantics.
   *
   * @param i the index of the bit
   * @return the bit at the given index
   */
  public PatternBit get(int i) {
    if (i < 0 || i >= width) {
      throw new IndexOutOfBoundsException(i);
    }
    final var idx = width - 1 - i;
    if (!mask.testBit(idx)) {
      return PatternBit.DONT_CARE;
    }
    return value.testBit(idx) ? PatternBit.ONE : PatternBit.ZERO;
  }

  /**
   * Tests whether the given bit vector matches this bit pattern.
   *
   * @param bitVector the bit vector to test
   * @return {@code true} if the bit vector matches this bit pattern, {@code false} otherwise
   */
  public boolean test(BitVector bitVector) {
    if (bitVector.width() != width()) {
      return false;
    }
    return bitVector.value().xor(value).and(mask).equals(BigInteger.ZERO);
  }

  /**
   * Creates a bit pattern from the given string representation. The string must consist of '0's,
   * '1's, which represent the corresponding bits. Any other character is interpreted as a <i>don't
   * care</i> bit.
   *
   * @param pattern The string representation of the bit pattern
   * @param width   The width of the bit pattern
   * @return The bit pattern
   */
  public static BitPattern fromString(String pattern, int width) {
    if (pattern.length() != width) {
      throw new IllegalArgumentException("Pattern length must match width");
    }
    BigInteger mask = BigInteger.ZERO;
    BigInteger value = BigInteger.ZERO;
    for (int i = 0; i < pattern.length(); i++) {
      final var idx = width - 1 - i;
      final var bit = pattern.charAt(i);
      if (bit == '0') {
        mask = mask.setBit(idx);
      } else if (bit == '1') {
        mask = mask.setBit(idx);
        value = value.setBit(idx);
      }
    }
    return new BitPattern(mask, value, width);
  }

  /**
   * Creates a bit pattern from the given value and maks, indicating which bits should are relevant.
   *
   * @param mask  The mask bits
   * @param value The value bits
   * @return The bit pattern.
   */
  public static BitPattern fromBitVector(BitVector mask, BitVector value) {
    if (mask.width() != value.width()) {
      throw new IllegalArgumentException("Mask and value must have the same width");
    }
    return new BitPattern(mask.value(), value.value(), mask.width());
  }

  /**
   * Creates an 'empty' bit pattern, i.e.: one where all bits are set to <i>don't care</i>.
   *
   * @param width The width of the bit pattern
   * @return The empty bit pattern
   */
  public static BitPattern empty(int width) {
    return new BitPattern(BigInteger.ZERO, BigInteger.ZERO, width);
  }

  /**
   * Convert a bit pattern to a bit vector. This is a helper method to convert the bit pattern with
   * potentially ignored (don't care) bits to a bit vector. All bits not set to 'don't care' will
   * be set to 1 in the resulting bit vector.
   *
   * @return the bit vector
   */
  public BitVector toMaskVector() {
    return new BitVector(mask, width);
  }

  /**
   * Convert a bit pattern to a bit vector. This is a helper method to convert the bit pattern with
   * potentially ignored (don't care) bits to a bit vector. The ignored bits in the pattern are
   * set to 0 in the resulting bit vector.
   *
   * @return the bit vector
   */
  public BitVector toBitVector() {
    return new BitVector(value, width);
  }

  /**
   * Left pads the bit vector with <i>don't care</i> bits.
   *
   * @param padding the padding
   * @return the padded bit pattern
   */
  public BitPattern leftPad(int padding) {
    if (padding <= 0) {
      return this;
    }
    final var targetWidth = width + padding;
    return new BitPattern(
        toMaskVector().leftPad(targetWidth, false).value(),
        toBitVector().leftPad(targetWidth, false).value(),
        targetWidth
    );
  }

  /**
   * Right pads the bit vector with <i>don't care</i> bits.
   *
   * @param padding the padding
   * @return the padded bit pattern
   */
  public BitPattern rightPad(int padding) {
    if (padding <= 0) {
      return this;
    }
    final var targetWidth = width + padding;
    return new BitPattern(
        toMaskVector().rightPad(targetWidth, false).value(),
        toBitVector().rightPad(targetWidth, false).value(),
        targetWidth
    );
  }

  /**
   * Truncates the pattern by removing higher order bits.
   *
   * @param width the width to truncate to
   * @return A bit pattern of the specified width
   */
  public BitPattern leftTrunc(int width) {
    final var m = widthMask(width);
    return new BitPattern(
        mask.and(m),
        value.and(m),
        width
    );
  }

  /**
   * Truncates the pattern by removing lower order bits.
   *
   * @param width the width to truncate to
   * @return A bit pattern of the specified width
   */
  public BitPattern rightTrunc(int width) {
    if (this.width < width) {
      throw new IllegalArgumentException("Target width must be leq than the actual width");
    }
    final var m = widthMask(width);
    return new BitPattern(
        mask.shiftRight(this.width - width).and(m),
        value.shiftRight(this.width - width).and(m),
        width
    );
  }

  /**
   * Returns whether this bit pattern matches all bits, i.e. all bits are <i>don't care</i>.
   *
   * @return {@code true} if all bits are <i>don't care</i>, {@code false} otherwise
   */
  public boolean doesMatchAll() {
    return mask.equals(BigInteger.ZERO);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BitPattern that)) {
      return false;
    }
    return width == that.width && Objects.equals(mask, that.mask)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mask, value, width);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < width(); i++) {
      PatternBit bit = get(i);
      sb.append(bit == PatternBit.ONE ? '1' : (
          bit == PatternBit.ZERO ? '0' : '-'));
    }
    return sb.toString();
  }
}
