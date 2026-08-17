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

package vadl.types;

import javax.annotation.Nullable;

/**
 * Represents all supported float encodings.
 */
public enum FloatEncoding {
  IEEE32(32, true),
  IEEE64(64, true);

  public final int size;
  public final boolean ieee;

  FloatEncoding(int size, boolean ieee) {
    this.size = size;
    this.ieee = ieee;
  }

  /**
   * Returns whether an IEEE encoding exists for the given size.
   */
  public static boolean isValidIEEESize(int size) {
    return size == 32 || size == 64;
  }

  /**
   * Returns the IEEE encoding for the given bit-size.
   */
  public static FloatEncoding ieee(int size) {
    return switch (size) {
      case 32 -> IEEE32;
      case 64 -> IEEE64;
      default -> throw new IllegalStateException("Unable to create IEEE encoding for size " + size);
    };
  }
}
