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

package vadl.iss.passes.tcgLowering;

/**
 * TcgWidth is an enumeration representing different width sizes in the context of the TCG.
 *
 * <p>This enum is utilized to denote the bit-width of variables through predefined constants.
 * It currently supports 32-bit (i32) and 64-bit (i64) widths.
 */
@SuppressWarnings("TypeName")
public enum Tcg_32_64 {
  i32(32),
  i64(64);

  public final int width;

  Tcg_32_64(int width) {
    this.width = width;
  }

  /**
   * Converts a given width in bits to the corresponding TcgWidth enumeration value.
   *
   * @param width The width in bits to convert.
   * @return The corresponding TcgWidth enumeration value.
   * @throws IllegalArgumentException If the given width does not match a known TcgWidth.
   */
  public static Tcg_32_64 fromWidth(int width) {
    return switch (width) {
      case 32 -> i32;
      case 64 -> i64;
      default -> throw new IllegalArgumentException("Invalid width: " + width);
    };
  }

  /**
   * Returns the next fitting type for the given width.
   */
  public static Tcg_32_64 nextFitting(int width) {
    if (width <= 32) {
      return Tcg_32_64.i32;
    } else if (width <= 64) {
      return Tcg_32_64.i64;
    } else {
      throw new IllegalArgumentException("Width too big: " + width);
    }
  }
}
