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
import vadl.viam.ViamError;


/**
 * The BigIntUtils class provides utility methods for performing operations
 * on big integers.
 */
public class BigIntUtils {

  /**
   * Calculates the two's complement of a given value with a specified bit width.
   *
   * @param value    The value for which the two's complement is to be calculated.
   * @param bitWidth The number of bits for the two's complement representation.
   * @return The two's complement of the given value.
   * @throws ViamError If the value does not fit in the specified bit width or is out of range.
   */
  public static BigInteger twosComplement(BigInteger value, int bitWidth) {
    if (value.bitLength() > bitWidth) {
      throw new ViamError(
          "Unexpected value %s for bit-width %s. Value does not fit in size.".formatted(value,
              bitWidth));
    }

    if (value.signum() >= 0) {
      // positive values are already in twos complement
      return value;
    }

    var oneBeforeWidth =
        BigInteger.ZERO.setBit(bitWidth);          // (msb|sign) msb-1 ... b0 := ( 1, 0, ..., 0 )
    var maxUnsigned =
        oneBeforeWidth.subtract(BigInteger.ONE);   // (msb|sign) msb-1 ... b0 := ( 0, 1, ..., 1 )
    var maxAbs =
        BigInteger.ZERO.setBit(bitWidth - 1);      // (msb|sign) msb-1 ... b0 := ( 0, 1, ..., 0 )

    var absValue = value.negate();

    if (absValue.compareTo(maxAbs) > 0) {
      throw new ViamError("Unexpected value %s for bit-width %s. Value is out of range (max: %s)."
          .formatted(value, bitWidth, maxAbs));
    }

    // to obtain the two's complement from an absValue, we apply
    // (abs( value ) - 1) ^ maxUnsigned
    return absValue.subtract(BigInteger.ONE).xor(maxUnsigned);
  }

  /**
   * Converts a BigInteger value from two's complement representation
   * to its original value, based on the provided BitsType.
   *
   * @param value    The value in two's complement representation.
   * @param bitWidth The bit-width of the value.
   * @param isSigned Whether the value is interpreted signed or unsigned.
   * @return The original value represented by the two's complement.
   */
  public static BigInteger fromTwosComplement(BigInteger value, int bitWidth, boolean isSigned) {
    var maxUnsigned = BigInteger.ZERO.setBit(bitWidth).subtract(BigInteger.ONE);
    var isSignBit = value.testBit(bitWidth - 1); // check if sign bit active

    if (isSigned && isSignBit) {
      //  negate  inverse        inc
      // - ((value ^ maxUnsigned) + 1)
      return value.xor(maxUnsigned).add(BigInteger.ONE).negate();
    }
    return value;
  }

  /**
   * Sets the bits in the specified range in the given BigInteger value.
   * The order of from and to is not important.
   * Both boundaries are inclusive.
   *
   * @param value The BigInteger value to set the bits in.
   * @param from  The starting index of the range (inclusive).
   * @param to    The ending index of the range (inclusive).
   * @return The BigInteger with the bits set in the specified range.
   */
  public static BigInteger setBitsInRange(BigInteger value, int from, int to) {
    var diff = Math.abs((from - to)) + 1;
    var lsb = Math.min(to, from);

    var mask = BigInteger.ONE.shiftLeft(diff)
        .subtract(BigInteger.ONE)
        .shiftLeft(lsb);

    return value.or(mask);
  }

  /**
   * Clears the bits in the specified range of a BigInteger value.
   * The order of 'from' and 'to' is not important.
   * Both boundaries are inclusive.
   *
   * @param value The BigInteger value to clear the bits in.
   * @param from  The starting index of the range (inclusive).
   * @param to    The ending index of the range (inclusive).
   * @return The BigInteger with the bits cleared in the specified range.
   */
  public static BigInteger clearBitsInRange(BigInteger value, int from, int to) {
    var diff = Math.abs((from - to)) + 1;
    var lsb = Math.min(to, from);

    var blackMask = BigInteger.ONE.shiftLeft(value.bitLength())
        .subtract(BigInteger.ONE);
    var whiteMask = BigInteger.ONE.shiftLeft(diff)
        .subtract(BigInteger.ONE)
        .shiftLeft(lsb);

    var mask = blackMask.xor(whiteMask);
    return value.and(mask);
  }

  /**
   * Calculates the mask value for a given length and left-shift value.
   * This is useful in combination of the {@link BigInteger#and(BigInteger)} method.
   *
   * @param len The number of bits in the mask.
   * @param lsh The number of bits to shift the mask to the left.
   * @return The calculated mask value.
   */
  public static BigInteger mask(int len, int lsh) {
    return BigInteger.ONE.shiftLeft(len).subtract(BigInteger.ONE).shiftLeft(lsh);
  }

  /**
   * Converts a given signed BigInteger value to an unsigned BigInteger value.
   *
   * @param value The signed BigInteger value to convert.
   * @return The corresponding unsigned BigInteger value.
   */
  public static BigInteger unsigned(BigInteger value) {
    if (value.signum() >= 0) {
      return value;
    }
    return new BigInteger(1, value.toByteArray());
  }

}
