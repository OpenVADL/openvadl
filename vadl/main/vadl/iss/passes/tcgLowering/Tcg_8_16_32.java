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

import static java.util.Objects.requireNonNull;

import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Tcg_8_16_32 is an enumeration representing various bit-widths used in
 * Tiny Code Generation (TCG). It supports 8-bit, 16-bit, and 32-bit widths.
 */
@SuppressWarnings("TypeName")
public enum Tcg_8_16_32 {
  i8(8),
  i16(16),
  i32(32);

  public final int width;

  Tcg_8_16_32(int width) {
    this.width = width;
  }

  /**
   * Converts a given width in bits to the corresponding Tcg_8_16_32 enumeration value.
   *
   * @param width The width in bits to convert.
   * @return The corresponding Tcg_8_16_32 enumeration value.
   * @throws IllegalArgumentException If the given width does not match a known Tcg_8_16_32
   *                                  enumeration value.
   */
  public static Tcg_8_16_32 fromWidth(int width) {
    return switch (width) {
      case 8 -> i8;
      case 16 -> i16;
      case 32 -> i32;
      default -> throw new IllegalArgumentException("Invalid width: " + width);
    };
  }

  /**
   * Returns the width of the expressions result type.
   */
  public static Tcg_8_16_32 from(ExpressionNode expr) {
    try {
      return fromWidth(expr.type().asDataType().bitWidth());
    } catch (Exception e) {
      throw new ViamGraphError(requireNonNull(e.getMessage()), e)
          .addContext(expr);
    }
  }

}
