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

package vadl.types.asmTypes;

import javax.annotation.Nullable;

/**
 * {@code @statements} represents a set of statements.
 * A statement is any one of all available {@code @instruction}.
 */
public class StatementsAsmType implements AsmType {
  @Nullable
  private static StatementsAsmType INSTANCE;

  private StatementsAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new StatementsAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "statements";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance();
  }

  @Override
  public String toCppTypeString(String prefix) {
    return "std::vector<NoData>";
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
