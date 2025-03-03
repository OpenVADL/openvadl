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
 * Interface representing an entity with a {@link SourceLocation}.
 *
 * <p>Provides a method to retrieve the source location associated with the implementing entity.
 */
public interface WithSourceLocation {
  /**
   * Retrieves the source location associated with the entity.
   *
   * @return the {@link SourceLocation} object that represents the location in the source code.
   */
  SourceLocation sourceLocation();
}
