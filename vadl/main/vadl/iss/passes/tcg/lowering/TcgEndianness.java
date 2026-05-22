// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.passes.tcg.lowering;

import vadl.viam.Endianness;

/**
 * Enum representing endianness for TCG (Tiny Code Generation) load/store operations.
 *
 * <p>The two modes are:
 * <ul>
 *    <li>BIG</li>
 *    <li>LITTLE</li>
 * </ul>
 */
public enum TcgEndianness {
  BIG,
  LITTLE;

  /**
   * Converts from {@link Endianness}.
   */
  public static TcgEndianness fromViamEndianness(Endianness endianness) {
    return switch (endianness) {
      case BIG -> BIG;
      case LITTLE -> LITTLE;
    };
  }

}
