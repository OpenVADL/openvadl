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

import static vadl.utils.MemOrderUtils.getLeBitPosition;
import static vadl.utils.MemOrderUtils.reverseByteOrder;

import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.viam.Constant.BitSlice;
import vadl.viam.Constant.BitSlice.Part;

public class MemOrderUtilsTest {

  @Test
  void testSwapByteOrder_byteAligned() {

    /* GIVEN */
    BigInteger v0 = BigInteger.ZERO;
    BigInteger v1 = new BigInteger("12", 16);
    BigInteger v2 = new BigInteger("1234", 16);
    BigInteger v3 = new BigInteger("123456", 16);
    BigInteger v4 = new BigInteger("12345678", 16);

    /* WHEN */
    BigInteger r0 = reverseByteOrder(v0, 0);
    BigInteger r1 = reverseByteOrder(v1, 8);
    BigInteger r2 = reverseByteOrder(v2, 16);
    BigInteger r3 = reverseByteOrder(v3, 24);
    BigInteger r4 = reverseByteOrder(v4, 32);

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
        () -> reverseByteOrder(BigInteger.ONE, 5));

    /* THEN */
    Assertions.assertEquals("Value of 5 bit is not byte aligned", e.getMessage());
  }

  @Test
  void testTranslateLittleEndianBitPosition() {

    // -------x
    Assertions.assertEquals(0, getLeBitPosition(0, 8));

    // x-------
    Assertions.assertEquals(7, getLeBitPosition(7, 8));

    // BE: -------x --------
    // LE: -------- -------x
    Assertions.assertEquals(0, getLeBitPosition(8, 16));

    // BE: ---x---- --------
    // LE: -------- ---x----
    Assertions.assertEquals(4, getLeBitPosition(12, 16));

    // BE: -------- ----x--- --------
    // LE: -------- ----x--- --------
    Assertions.assertEquals(11, getLeBitPosition(11, 24));

    // BE: ----x--- -------- --------
    // LE: -------- -------- ----x---
    Assertions.assertEquals(3, getLeBitPosition(19, 24));

    // BE: -------- -------- ----x---
    // LE: ----x--- -------- --------
    Assertions.assertEquals(19, getLeBitPosition(3, 24));

    // BE: -------- -------- -------- ----x---
    // LE: ----x--- -------- -------- --------
    Assertions.assertEquals(27, getLeBitPosition(3, 32));
  }

  @Test
  void testTranslateByteOrder_bitSlice() {

    /* GIVEN */
    BitSlice s0 = new BitSlice(Part.of(3, 0));

    // BE: -----xxx xx------
    // LE: xx------ -----xxx
    BitSlice s1 = new BitSlice(Part.of(10, 6));

    // BE: -12----- -----34-
    // LE: -----34- -12-----
    BitSlice s2 = new BitSlice(Part.of(14, 13), Part.of(2, 1));

    // BE: -34----- -----12-
    // LE: -----12- -34-----
    BitSlice s3 = new BitSlice(Part.of(2, 1), Part.of(14, 13));

    /* WHEN */
    BitSlice r0 = reverseByteOrder(s0, 8);
    BitSlice r1 = reverseByteOrder(s1, 16);
    BitSlice r2 = reverseByteOrder(s2, 16);
    BitSlice r3 = reverseByteOrder(s3, 16);

    /* THEN */

    BitSlice e0 = new BitSlice(Part.of(3, 0));
    Assertions.assertEquals(e0, r0);

    BitSlice e1 = new BitSlice(Part.of(2, 0), Part.of(15, 14));
    Assertions.assertEquals(e1, r1);

    BitSlice e2 = new BitSlice(Part.of(6, 5), Part.of(10, 9));
    Assertions.assertEquals(e2, r2);

    BitSlice e3 = new BitSlice(Part.of(10, 9), Part.of(6, 5));
    Assertions.assertEquals(e3, r3);
  }
}
