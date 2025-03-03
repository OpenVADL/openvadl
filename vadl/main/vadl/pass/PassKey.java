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

package vadl.pass;

/**
 * This the typed name of an identifier of a {@link Pass}.
 * Note that the difference between {@link PassName} and {@link PassKey} is that {@link PassKey}
 * must be unique in the {@link PassManager}. However, it should be possible to schedule the same
 * {@link Pass} with the same {@link PassName} multiple times.
 */
public record PassKey(String value) {

  public static PassKey of(String value) {
    return new PassKey(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
