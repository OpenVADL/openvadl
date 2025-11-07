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

package vadl.vdt.target.rtl;

import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.PBit;

public class ChiselUtils {

  private ChiselUtils() {
    // No instances
  }

  public static CharSequence toChiselPattern(BitPattern pattern, boolean padding) {
    final var sb = new StringBuilder("b");

    boolean isLeadingWildcard = true;
    for (int i = 0; i < pattern.width(); i++) {
      if (pattern.get(i).getValue() == PBit.Value.DONT_CARE && isLeadingWildcard && !padding) {
        continue;
      }
      isLeadingWildcard = false;
      if (pattern.get(i).getValue() == PBit.Value.ONE) {
        sb.append('1');
      } else if (pattern.get(i).getValue() == PBit.Value.ZERO) {
        sb.append('0');
      } else {
        sb.append('?');
      }
    }
    return sb;
  }

}
