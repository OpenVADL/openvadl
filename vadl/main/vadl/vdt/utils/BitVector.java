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
import java.util.Objects;

/**
 * Represents a bit vector, i.e. a sequence of bits.
 */
public class BitVector implements Vector<Bit>, BitWise<BitVector> {

  private final Bit[] bits;

  /**
   * Creates a new bit vector from the given bits.
   *
   * @param bits the bits of the vector
   */
  public BitVector(Bit[] bits) {
    this.bits = bits;
  }

  /**
   * Creates a new bit vector from the given bits.
   *
   * @param bits the bits of the vector
   */
  public BitVector(boolean[] bits) {
    this.bits = new Bit[bits.length];
    for (int i = 0; i < bits.length; i++) {
      this.bits[i] = new Bit(bits[i]);
    }
  }

  /**
   * Creates a bit vector from the given value.
   *
   * @param value the value as a big integer
   * @param width the width of the bit vector to create
   * @return the bit vector
   */
  public static BitVector fromValue(BigInteger value, int width) {
    final Bit[] bits = new Bit[width];
    for (int i = 0; i < width; i++) {
      bits[i] = new Bit(value.testBit(width - 1 - i));
    }
    return new BitVector(bits);
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
    final Bit[] bits = new Bit[width];
    for (int i = 0; i < width; i++) {
      if (i < value.length()) {
        if (value.charAt(i) != '0' && value.charAt(i) != '1') {
          throw new IllegalArgumentException("Invalid character in value");
        }
        bits[i] = new Bit(value.charAt(i) == '1');
      } else {
        bits[i] = new Bit(false);
      }
    }
    return new BitVector(bits);
  }

  /**
   * Converts the bit vector to its corresponding BigInteger representation.
   *
   * @return the value as a big integer
   */
  public BigInteger toValue() {
    BigInteger value = BigInteger.ZERO;
    for (int i = 0; i < width(); i++) {
      if (get(width() - (i + 1)).value()) {
        value = value.setBit(i);
      }
    }
    return value;
  }

  @Override
  public int width() {
    return bits.length;
  }

  @Override
  public Bit get(int i) {
    return bits[i];
  }

  @Override
  public BitVector and(BitVector other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).and(other.get(i));
    }
    return new BitVector(result);
  }

  @Override
  public BitVector or(BitVector other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).or(other.get(i));
    }
    return new BitVector(result);
  }

  @Override
  public BitVector xor(BitVector other) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).xor(other.get(i));
    }
    return new BitVector(result);
  }

  @Override
  public BitVector not() {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i).not();
    }
    return new BitVector(result);
  }

  /**
   * Shifts the bit vector to the left by n bits. The bits that are shifted out are lost.
   *
   * @param n    the number of bits to shift
   * @param fill the value to fill the shifted out bits with (true for 1, false for 0)
   * @return the shifted bit vector
   */
  public BitVector shiftLeft(int n, boolean fill) {
    final Bit[] result = new Bit[width()];
    for (int i = n; i < width(); i++) {
      result[i - n] = get(i);
    }
    for (int i = width() - n; i < width(); i++) {
      result[i] = fill ? new Bit(true) : new Bit(false);
    }
    return new BitVector(result);
  }

  /**
   * Shifts the bit vector to the right by n bits. The bits that are shifted out are lost.
   *
   * @param n    the number of bits to shift
   * @param fill the value to fill the shifted out bits with (true for 1, false for 0)
   * @return the shifted bit vector
   */
  public BitVector shiftRight(int n, boolean fill) {
    final Bit[] result = new Bit[width()];
    for (int i = 0; i < n; i++) {
      result[i] = fill ? new Bit(true) : new Bit(false);
    }
    for (int i = n; i < width(); i++) {
      result[i] = get(i - n);
    }
    return new BitVector(result);
  }

  /**
   * Left pads the bit vector with the given fill value until it reaches the target width. The
   *
   * @param target the target width
   * @param fill   the value to fill with
   * @return the padded bit vector
   */
  public BitVector leftPad(int target, Bit fill) {
    if (target <= width()) {
      return this;
    }
    final Bit[] result = new Bit[target];
    for (int i = 0; i < target - width(); i++) {
      result[i] = fill;
    }
    for (int i = target - width(); i < target; i++) {
      result[i] = get(i - (target - width()));
    }
    return new BitVector(result);
  }

  /**
   * Right pads the bit vector with the given fill value until it reaches the target width. The
   *
   * @param target the target width
   * @param fill   the value to fill with
   * @return the padded bit vector
   */
  public BitVector rightPad(int target, Bit fill) {
    if (target <= width()) {
      return this;
    }
    final Bit[] result = new Bit[target];
    for (int i = 0; i < width(); i++) {
      result[i] = get(i);
    }
    for (int i = width(); i < target; i++) {
      result[i] = fill;
    }
    return new BitVector(result);
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
    final Bit[] result = new Bit[length];
    for (int i = 0; i < length; i++) {
      result[i] = get(offset + i);
    }
    return new BitVector(result);
  }

  @Override
  public int hashCode() {
    int result = 1;
    for (int i = 0; i < width(); i++) {
      result = 31 * result + Objects.hashCode(get(i));
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BitVector other = (BitVector) obj;
    if (width() != other.width()) {
      return false;
    }
    for (int i = 0; i < width(); i++) {
      if (!Objects.equals(get(i), other.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < width(); i++) {
      builder.append(get(i).value() ? '1' : '0');
    }
    return builder.toString();
  }
}
