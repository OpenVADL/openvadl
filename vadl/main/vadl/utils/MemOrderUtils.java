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

package vadl.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import vadl.viam.Constant.BitSlice;
import vadl.viam.Constant.BitSlice.Part;

/**
 * Utility methods to convert between bit- and little-endian memory order.
 */
public class MemOrderUtils {

  private MemOrderUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * For a bit-position (of a bit vector in big endian byte order) calculate the corresponding
   * little-endian position of the bit.
   *
   * @param idx   The index to translate
   * @param width The width of the bit-vector
   * @return The translated bit position in little endian byte order
   */
  public static int getLeBitPosition(int idx, int width) {

    if (idx < 0 || width < 0) {
      throw new IllegalArgumentException("Invalid index or width: " + idx + "/" + width);
    }

    if (idx >= width) {
      throw new IndexOutOfBoundsException(idx);
    }

    final int alignedWidth = width % 8 != 0 ? width + (8 - width % 8) : width;
    final int byteWidth = alignedWidth / 8;

    if (byteWidth <= 1) {
      return idx;
    }

    return (byteWidth - (idx / 8) - 1) * 8 + idx % 8;
  }

  /**
   * Reverse the byte order of the given value.
   *
   * @param value    The value to reverse.
   * @param bitWidth The number of bits to consider (Must be a multiple of 8);
   * @return The reversed value.
   */
  public static BigInteger reverseByteOrder(BigInteger value, int bitWidth) {

    if (bitWidth < 0) {
      throw new IllegalArgumentException("Negative bit-width: " + bitWidth);
    }

    if (bitWidth % 8 != 0) {
      throw new IllegalArgumentException(
          "Value of %d bit is not byte aligned".formatted(bitWidth));
    }

    BigInteger result = BigInteger.ZERO;

    for (int i = 0; i < bitWidth; i++) {
      if (value.testBit(i)) {
        result = result.setBit(getLeBitPosition(i, bitWidth));
      }
    }

    return result;
  }

  /**
   * Translate the bit slice to a byte-swapped bit vector.
   *
   * @param slice    The bit slice to translate
   * @param bitWidth The total bit width (Must be a multiple of 8)
   * @return The translated bit slice.
   */
  public static BitSlice reverseByteOrder(BitSlice slice, int bitWidth) {

    if (bitWidth < 0) {
      throw new IllegalArgumentException("Negative bit-width: " + bitWidth);
    }

    if (bitWidth % 8 != 0) {
      throw new IllegalArgumentException(
          "Value of %d bit is not byte aligned".formatted(bitWidth));
    }

    final int[] idx = slice.stream()
        .map(i -> getLeBitPosition(i, bitWidth))
        .toArray();

    final List<Part> parts = new ArrayList<>();
    Part current = null;
    for (int i : idx) {
      if (current == null) {
        current = new Part(i, i);
        continue;
      }

      if (i - 1 == current.msb() || i + 1 == current.lsb()) {
        current = new Part(i, i).join(current);
        continue;
      }

      parts.add(current);
      current = new Part(i, i);
    }

    if (current != null) {
      parts.add(current);
    }

    return new BitSlice(parts.toArray(new Part[0]));
  }
}
