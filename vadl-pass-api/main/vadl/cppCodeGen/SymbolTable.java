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

/**
 * This is a helper class for generating variable names for the OOP layer.
 */
public class SymbolTable {
  private final String prefix;
  private int state = 0;

  public SymbolTable() {
    prefix = "";
  }

  /**
   * Sets a prefix for the variables.
   */
  public SymbolTable(String prefix) {
    this.prefix = prefix;
  }

  /**
   * Offset the SymbolTable so it starts not with "a".
   */
  public SymbolTable(int offset) {
    this.state = offset;
    this.prefix = "";
  }

  /**
   * Get a symbol without modifying the state.
   */
  public String getLastVariable() {
    return prefix + getVariableBasedOnState(state);
  }

  /**
   * Generate a variable name. For example, "a", "ab", "xy" etc.
   */
  public String getNextVariable() {
    return prefix + getVariableBasedOnState(state++);
  }

  /**
   * Generates a variable for a given index without any prefix.
   */
  public static String getVariableBasedOnState(int i) {
    return i < 0 ? "" : getVariableBasedOnState((i / 26) - 1) + (char) (97 + i % 26);
  }
}
