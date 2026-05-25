// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BitVectorTest {

  @Test
  void testLeftPad() {
    /* GIVEN */
    var bv = BitVector.fromString("101", 8);

    /* WHEN */
    BitVector result = bv.leftPad(8, false);

    /* THEN */
    Assertions.assertEquals(9, result.width());
    Assertions.assertEquals("00000101", result.toString());
  }
}
