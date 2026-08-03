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

package vadl.viam;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum representing float exception flags.
 *
 * <p>The supported flags are:
 * <ul>
 *    <li>INVALID</li>
 *    <li>DIV_BY_ZERO</li>
 *    <li>OVERFLOW</li>
 *    <li>UNDERFLOW</li>
 *    <li>INEXACT</li>
 * </ul>
 */
public enum FloatExceptionFlag {
  INVALID("invalid", 0),
  DIV_BY_ZERO("div_by_zero", 1),
  OVERFLOW("overflow", 2),
  UNDERFLOW("underflow", 3),
  INEXACT("inexact", 4);

  public final String name;

  /**
   * See softfloat-types.h
   */
  public final int qemuFlagOffset;

  FloatExceptionFlag(String name, int qemuFlagOffset) {
    this.name = name;
    this.qemuFlagOffset = qemuFlagOffset;
  }

  public static Optional<FloatExceptionFlag> from(String name) {
    return Arrays.stream(values()).filter(f -> f.name.equals(name)).findFirst();
  }
}
