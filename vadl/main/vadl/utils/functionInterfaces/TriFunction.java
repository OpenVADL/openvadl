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

package vadl.utils.functionInterfaces;

import java.util.Objects;
import java.util.function.Function;

/**
 * A Java function interface with 3 parameter and a return value.
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
  R apply(A var1, B var2, C var3);

  default <V> TriFunction<A, B, C, V> andThen(
      Function<? super R, ? extends V> after) {
    Objects.requireNonNull(after);
    return (a, b, c) -> after.apply(this.apply(a, b, c));
  }
}