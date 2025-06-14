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

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a bit pattern, which is a vector of bits where each bit can be either 0, 1 or <i>don't
 * care</i>.
 */
public class BitPattern implements Vector<PBit>, Predicate<BitVector> {

  private final PBit[] bits;

  public BitPattern(PBit[] bits) {
    this.bits = bits;
  }

  @Override
  public int width() {
    return bits.length;
  }

  @Override
  public PBit get(int i) {
    return bits[i];
  }

  @Override
  public boolean test(BitVector bitVector) {
    if (bitVector.width() != width()) {
      return false;
    }
    for (int i = 0; i < width(); i++) {
      if (get(i).getValue() != PBit.Value.DONT_CARE && (
          (get(i).getValue() == PBit.Value.ONE && !bitVector.get(i).value()) || (
              get(i).getValue() == PBit.Value.ZERO && bitVector.get(i).value()))) {
        return false;
      }
    }
    return true;
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
    final PBit[] bits = new PBit[width];
    if (pattern.length() != width) {
      throw new IllegalArgumentException("Pattern length must match width");
    }
    for (int i = 0; i < pattern.length(); i++) {
      bits[i] = new PBit(pattern.charAt(i) == '1' ? PBit.Value.ONE
          : (pattern.charAt(i) == '0' ? PBit.Value.ZERO : PBit.Value.DONT_CARE));
    }
    return new BitPattern(bits);
  }

  /**
   * Creates an 'empty' bit pattern, i.e.: one where all bits are set to <i>don't care</i>.
   *
   * @param width The width of the bit pattern
   * @return The empty bit pattern
   */
  public static BitPattern empty(int width) {
    final PBit[] bits = new PBit[width];
    for (int i = 0; i < width; i++) {
      bits[i] = new PBit(PBit.Value.DONT_CARE);
    }
    return new BitPattern(bits);
  }

  /**
   * Convert a bit pattern to a bit vector. This is a helper method to convert the bit pattern with
   * potentially ignored (don't care) bits to a bit vector. All bits not set to 'don't care' will
   * be set to 1 in the resulting bit vector.
   *
   * @return the bit vector
   */
  public BitVector toMaskVector() {
    Bit[] veBits = new Bit[width()];
    for (int i = 0; i < veBits.length; i++) {
      veBits[i] = new Bit(bits[i].getValue() != PBit.Value.DONT_CARE);
    }
    return new BitVector(veBits);
  }

  /**
   * Convert a bit pattern to a bit vector. This is a helper method to convert the bit pattern with
   * potentially ignored (don't care) bits to a bit vector. The ignored bits in the pattern are
   * set to 0 in the resulting bit vector.
   *
   * @return the bit vector
   */
  public BitVector toBitVector() {
    Bit[] veBits = new Bit[width()];
    for (int i = 0; i < veBits.length; i++) {
      veBits[i] = new Bit(bits[i].getValue() == PBit.Value.ONE);
    }
    return new BitVector(veBits);
  }

  /**
   * Returns whether this bit pattern matches all bits, i.e. all bits are <i>don't care</i>.
   *
   * @return {@code true} if all bits are <i>don't care</i>, {@code false} otherwise
   */
  public boolean doesMatchAll() {
    for (int i = 0; i < width(); i++) {
      if (get(i).getValue() != PBit.Value.DONT_CARE) {
        return false;
      }
    }
    return true;
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
    final BitPattern other = (BitPattern) obj;
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
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < width(); i++) {
      sb.append(get(i).getValue() == PBit.Value.ONE ? '1' : (
          get(i).getValue() == PBit.Value.ZERO ? '0' : '-'));
    }
    return sb.toString();
  }
}
