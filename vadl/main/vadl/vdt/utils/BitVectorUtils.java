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

/**
 * Common bit vector operations.
 */
public class BitVectorUtils {

  private BitVectorUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Find the smallest power of two that is greater or equal to n.
   *
   * @param n the input number
   * @return the smallest fitting power of two
   */
  public static int fittingPowerOfTwo(int n) {
    final BigInteger bigN = BigInteger.valueOf(n);
    if (bigN.compareTo(BigInteger.ZERO) <= 0) {
      throw new IllegalArgumentException("Input must be a positive integer");
    }
    if (bigN.getLowestSetBit() == bigN.bitLength() - 1) {
      // n is already a power of two
      return n;
    }
    return BigInteger.ONE.shiftLeft(bigN.bitLength()).intValue();
  }
}
