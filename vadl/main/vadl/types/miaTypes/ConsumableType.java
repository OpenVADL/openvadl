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

package vadl.types.miaTypes;

import vadl.types.Type;

/**
 * Marker interface for micro architecture types that can be used with the
 * {@code consume} builtin.
 */
public abstract class ConsumableType extends MicroArchitectureType {

  /**
   * The type returned by the {@code consume} builtin when an argument of this
   * type is provided as the first argument.
   *
   * <p>If this returns {@link vadl.types.VoidType} when used as the first
   * argument to {@code consume}, an error {@link vadl.error.Diagnostic} is
   * generated during typechecking. This is useful for e.g. generating an error
   * for stages that have no output type.
   *
   *  @return The type that should be returned by {@code consume}.
   */
  public abstract Type consumedType();
}
