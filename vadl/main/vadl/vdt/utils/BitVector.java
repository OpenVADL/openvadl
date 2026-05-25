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
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a bit vector, i.e. a sequence of bits.
 */
public class BitVector {

  private final BigInteger value;
  private final int width;

  /**
   * Creates a new bit vector from the given value.
   *
   * @param value the bits of the vector
   */
  public BitVector(BigInteger value, int width) {
    if (width < 0) {
      throw new IllegalArgumentException("Width must be non-negative");
    }
    this.value = Objects.requireNonNull(value).and(widthMask(width));
    this.width = width;
  }

  /**
   * Creates a bit vector from the given string representation. The string must consist of '0's and
   * '1's, which represent the corresponding bits.
   *
   * @param value the string representation of the bit vector
   * @param width the width of the bit vector to create
   * @return the bit vector
   */
  public static BitVector fromString(String value, int width) {
    if (width < 0) {
      throw new IllegalArgumentException("Width must be non-negative");
    }
    final String prefix = value.substring(0, Math.min(value.length(), width));
    for (int i = 0; i < prefix.length(); i++) {
      if (prefix.charAt(i) != '0' && prefix.charAt(i) != '1') {
        throw new IllegalArgumentException("Invalid character in value");
      }
    }
    final String sanitized = StringUtils.rightPad(prefix, width, '0');
    final var val = sanitized.isEmpty() ? BigInteger.ZERO : new BigInteger(sanitized, 2);
    return new BitVector(val, width);
  }

  public BigInteger value() {
    return value;
  }

  public int width() {
    return width;
  }

  public boolean get(int i) {
    if (i < 0 || i >= width) {
      throw new IndexOutOfBoundsException(i);
    }
    return value.testBit(width - 1 - i);
  }

  public BitVector and(BitVector other) {
    if (width != other.width) {
      throw new IllegalArgumentException("Bit vectors must have the same width");
    }
    return new BitVector(value.and(other.value), width);
  }

  public BitVector or(BitVector other) {
    if (width != other.width) {
      throw new IllegalArgumentException("Bit vectors must have the same width");
    }
    return new BitVector(value.or(other.value), width);
  }

  public BitVector xor(BitVector other) {
    if (width != other.width) {
      throw new IllegalArgumentException("Bit vectors must have the same width");
    }
    return new BitVector(value.xor(other.value), width);
  }

  public BitVector not() {
    return new BitVector(value.xor(widthMask(width)), width);
  }

  /**
   * Left pads the bit vector with the given fill value until it reaches the target width. The
   *
   * @param target the target width
   * @param fill   the value to fill with
   * @return the padded bit vector
   */
  public BitVector leftPad(int target, boolean fill) {
    if (target <= width()) {
      return this;
    }
    if (!fill) {
      return new BitVector(value, target);
    }
    final var mask = BigInteger.ONE
        .shiftLeft(target - width)
        .subtract(BigInteger.ONE)
        .shiftLeft(width);
    return new BitVector(value.or(mask), target);
  }

  /**
   * Right pads the bit vector with the given fill value until it reaches the target width. The
   *
   * @param target the target width
   * @param fill   the value to fill with
   * @return the padded bit vector
   */
  public BitVector rightPad(int target, boolean fill) {
    if (target <= width()) {
      return this;
    }

    if (!fill) {
      return new BitVector(value.shiftLeft(target - width), target);
    }

    final var mask = BigInteger.ONE
        .shiftLeft(target - width)
        .subtract(BigInteger.ONE);
    return new BitVector(value.shiftLeft(target - width).or(mask), target);
  }

  /**
   * Truncates the bit vector to the given length, starting at the given offset. The bits that are
   * not included in the truncated vector are lost.
   *
   * @param offset the offset to start truncating from
   * @param length the length of the truncated vector
   * @return the truncated bit vector
   */
  public BitVector truncate(int offset, int length) {
    if (offset < 0 || length < 0 || offset + length > width()) {
      throw new IllegalArgumentException("Invalid offset or length for truncation");
    }
    final var mask = BigInteger.ONE
        .shiftLeft(length)
        .subtract(BigInteger.ONE);
    final var v = value.shiftRight(width - offset - length).and(mask);
    return new BitVector(v, length);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BitVector bitVector)) {
      return false;
    }
    return width == bitVector.width && Objects.equals(value, bitVector.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, width);
  }

  @Override
  public String toString() {
    if (width == 0) {
      return "";
    }
    return StringUtils.leftPad(value.toString(2), width, '0');
  }
}
