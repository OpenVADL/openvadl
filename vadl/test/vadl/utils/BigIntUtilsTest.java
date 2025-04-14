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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BigIntUtilsTest {

  @Test
  void testSwapByteOrder_byteAligned() {

    /* GIVEN */
    BigInteger v0 = BigInteger.ZERO;
    BigInteger v1 = new BigInteger("12", 16);
    BigInteger v2 = new BigInteger("1234", 16);
    BigInteger v3 = new BigInteger("123456", 16);
    BigInteger v4 = new BigInteger("12345678", 16);

    /* WHEN */
    BigInteger r0 = BigIntUtils.reverseByteOrder(v0, 0);
    BigInteger r1 = BigIntUtils.reverseByteOrder(v1, 8);
    BigInteger r2 = BigIntUtils.reverseByteOrder(v2, 16);
    BigInteger r3 = BigIntUtils.reverseByteOrder(v3, 24);
    BigInteger r4 = BigIntUtils.reverseByteOrder(v4, 32);

    /* THEN */
    Assertions.assertEquals("0", r0.toString(16));
    Assertions.assertEquals("12", r1.toString(16));
    Assertions.assertEquals("3412", r2.toString(16));
    Assertions.assertEquals("563412", r3.toString(16));
    Assertions.assertEquals("78563412", r4.toString(16));
  }

  @Test
  void testSwapByteOrder_notAligned() {

    /* WHEN */
    IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
        () -> BigIntUtils.reverseByteOrder(BigInteger.ONE, 5));

    /* THEN */
    Assertions.assertEquals("Value of 5 bit is not byte aligned", e.getMessage());
  }
}
