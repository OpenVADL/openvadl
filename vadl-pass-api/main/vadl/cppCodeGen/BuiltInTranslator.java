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

package vadl.cppCodeGen;

import static vadl.viam.ViamError.ensure;

import vadl.types.BuiltInTable;

/**
 * Not all Builtin's operators are valid cpp operands.
 */
public class BuiltInTranslator {
  /**
   * Map a builtin to a string.
   */
  public static String map(BuiltInTable.BuiltIn built) {
    if (built == BuiltInTable.EQU) {
      return "==";
    }

    ensure(built.operator() != null, "operator must be null");
    return built.operator();
  }
}
