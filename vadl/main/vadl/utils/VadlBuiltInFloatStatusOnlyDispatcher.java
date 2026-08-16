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

import vadl.types.BuiltInTable;

/**
 * A dispatcher that handles all {@code VADL::F*S} built-ins.
 */
public interface VadlBuiltInFloatStatusOnlyDispatcher<T> {

  /**
   * Calls the correct handler for the given built-in and uses the input as argument.
   *
   * @param input   is passed to the handler method.
   * @param builtIn to find the correct handler method.
   * @return true if the handler was found and called, false otherwise.
   */
  default boolean dispatch(T input, BuiltInTable.BuiltIn builtIn) {
    if (builtIn == BuiltInTable.FADDS) {
      handleFADDS(input);
    } else {
      return false;
    }
    return true;
  }

  void handleFADDS(T input);


}
