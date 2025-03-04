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

  void handleADDS(T input);

  void handleADDC(T input);

  void handleSSATADDS(T input);

  void handleUSATADDS(T input);

  void handleSSATADDC(T input);

  void handleUSATADDC(T input);

  void handleSUBSC(T input);

  void handleSUBSB(T input);

  void handleSUBC(T input);

  void handleSUBB(T input);

  void handleSSATSUBS(T input);

  void handleUSATSUBS(T input);

  void handleSSATSUBC(T input);

  void handleUSATSUBC(T input);

  void handleSSATSUBB(T input);

  void handleUSATSUBB(T input);

  void handleMULS(T input);

  void handleSMULLS(T input);

  void handleUMULLS(T input);

  void handleSUMULLS(T input);

  void handleSMODS(T input);

  void handleUMODS(T input);

  void handleSDIVS(T input);

  void handleUDIVS(T input);

  void handleANDS(T input);

  void handleXORS(T input);

  void handleORS(T input);

  void handleLSLS(T input);

  void handleLSLC(T input);

  void handleASRS(T input);

  void handleLSRS(T input);

  void handleASRC(T input);

  void handleLSRC(T input);

  void handleROLS(T input);

  void handleROLC(T input);

  void handleRORS(T input);

  void handleRORC(T input);


}
