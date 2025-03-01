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
 * A helper construct to define a tuple.
 */
public class Triple<T, X, Z> {
  private final T left;
  private final X middle;
  private final Z right;

  /**
   * Tuple constructor for three parameters.
   */
  public Triple(T left, X middle, Z right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  public static <T, X, Z> Triple<T, X, Z> of(T left, X middle, Z right) {
    return new Triple<T, X, Z>(left, middle, right);
  }

  public Z right() {
    return right;
  }

  public T left() {
    return left;
  }

  public X middle() {
    return middle;
  }
}
