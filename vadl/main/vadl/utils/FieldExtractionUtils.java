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

import static vadl.utils.MemOrderUtils.reverseByteOrder;

import java.math.BigInteger;
import java.nio.ByteOrder;
import vadl.viam.Constant.Value;
import vadl.viam.Format;

/**
 * Utility class to extract format fields from an instruction encoding.
 */
public class FieldExtractionUtils {

  private FieldExtractionUtils() {
    throw new IllegalStateException("Static utility class");
  }

  /**
   * Extract the value of an instruction's format field from the encoding the specified byte order.
   *
   * @param encoding  The encoding of the full instruction.
   * @param byteOrder The byte order of the encoding.
   * @param field     The field to extract.
   * @return The encoded value of the given field.
   */
  public static Value extract(BigInteger encoding, ByteOrder byteOrder, Format.Field field) {

    // For LE encoding, swap the byte order
    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
      encoding = reverseByteOrder(encoding, field.format().type().bitWidth());
    }

    BigInteger value = BigInteger.ZERO;

    int offset = field.size();
    for (Integer idx : field.bitSlice()) {
      if (encoding.testBit(idx)) {
        value = value.setBit(offset - 1);
      }
      offset--;
    }

    return Value.fromInteger(value, field.type());
  }

  /**
   * Extract the value of an instruction's field access function from the encoding in the specified
   * byte order.
   *
   * @param encoding    The encoding of the full instruction.
   * @param fieldAccess The field access to evaluate.
   * @return The value returned by the given field access.
   */
  public static Value extract(BigInteger encoding, ByteOrder byteOrder,
                              Format.FieldAccess fieldAccess) {

    // TODO: Implement field access evaluation
    return Value.of(false);
  }
}
