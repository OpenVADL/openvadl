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

/**
 * Utility class for {@link StringBuilder}.
 */
public class StringBuilderUtils {

  private StringBuilderUtils() {
    // Utility class
  }

  /**
   * Joins the elements of the provided {@link Iterable} into a single {@link StringBuilder}
   * containing the provided delimiter between each element.
   *
   * @param delimiter The delimiter that separates each element.
   * @param elements  The elements to join together.
   * @return A {@link StringBuilder} containing the joined elements.
   */
  public static StringBuilder join(CharSequence delimiter,
                                   Iterable<? extends CharSequence> elements) {
    final StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (CharSequence element : elements) {
      if (first) {
        first = false;
      } else {
        sb.append(delimiter);
      }
      sb.append(element);
    }
    return sb;
  }

}
