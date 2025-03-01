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
public class Quadruple<T, X, Z, Y> {
  private final T first;
  private final X second;
  private final Z third;
  private final Y fourth;

  /**
   * Tuple constructor for four parameters.
   */
  public Quadruple(T first, X second, Z third, Y fourth) {
    this.first = first;
    this.second = second;
    this.third = third;
    this.fourth = fourth;
  }

  public T first() {
    return first;
  }

  public X second() {
    return second;
  }

  public Z third() {
    return third;
  }

  public Y fourth() {
    return fourth;
  }
}
