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
 * A dispatcher that handles all {@code VADL::*} built-ins.
 */
public interface VadlBuiltInDispatcher<T>
    extends VadlBuiltInNoStatusDispatcher<T>, VadlBuiltInStatusOnlyDispatcher<T> {

  @Override
  default boolean dispatch(T input, BuiltInTable.BuiltIn builtIn) {
    if (VadlBuiltInNoStatusDispatcher.super.dispatch(input, builtIn)) {
      return true;
    } else {
      return VadlBuiltInStatusOnlyDispatcher.super.dispatch(input, builtIn);
    }
  }

}
