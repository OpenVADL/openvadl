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

/**
 * Models an input/output dependency between two components of the MiA,
 * {@link #source()} and {@link #destination()}.
 *
 * <p>The implementations limit the kinds of MiA elements can be in an
 * input/output relation with each other, serving as a sort of documentation
 * while also allowing for elegant switch statements/expressions.
 */
public sealed interface MiaDependency permits MiaDependency.StageToStageOutputDependency {

  /**
   * Gets the source of this dependency. The exact type of this depends on the
   * implementation. If you need it, that's a good sign that you might want to
   * use a switch over the {@code MiaDependency}.
   *
   * @return The source of this dependency.
   */
  Definition source();

  /**
   * Gets the destination of this dependency. The exact type of this depends on
   * the implementation. If you need it, that's a good sign that you might want
   * to use a switch over the {@code MiaDependency}.
   *
   * @return The destination of this dependency.
   */
  Definition destination();

  /**
   * Models a dependency from a {@link Stage} to a {@link StageOutput}.
   *
   * <p>Such dependencies arise from reads of the source stage output from the
   * destination stage's {@link Stage#behavior() behaviour}. Multiple reads of
   * the same stage output inside the same stage are collapsed into a single
   * dependency.
   *
   * @param source The read {@code StageOutput}.
   * @param destination The reading {@code Stage}.
   */
  record StageToStageOutputDependency(
      StageOutput source,
      Stage destination
  ) implements MiaDependency {}
}
