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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class StreamUtilsTest {
  @Test
  public void directionalRangeAscendingOrder() {
    assertArrayEquals(new int[] {1, 2, 3, 4, 5}, StreamUtils.directionalRange(1, 6).toArray());
  }

  @Test
  public void directionalRangeDescendingOrder() {
    assertArrayEquals(new int[] {5, 4, 3, 2, 1}, StreamUtils.directionalRange(5, 0).toArray());
  }

  @Test
  public void directionalRangeSingleValue() {
    assertArrayEquals(new int[] {}, StreamUtils.directionalRange(0, 0).toArray());
  }

  @Test
  public void directionalRangeClosedAscendingOrder() {
    assertArrayEquals(new int[] {1, 2, 3, 4, 5},
        StreamUtils.directionalRangeClosed(1, 5).toArray());
  }

  @Test
  public void directionalRangeClosedDescendingOrder() {
    assertArrayEquals(new int[] {5, 4, 3, 2, 1},
        StreamUtils.directionalRangeClosed(5, 1).toArray());
  }

  @Test
  public void directionalRangeClosedSingleValue() {
    assertArrayEquals(new int[] {0}, StreamUtils.directionalRangeClosed(0, 0).toArray());
  }

}
