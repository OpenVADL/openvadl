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
 * {@code @modifier} refers to a relocation defined
 * in the modifiers section of the assembly description.
 *
 * <p>The combination of a {@code @modifier}
 * and an {@code @expression} can be cast to {@code @operand}.</p>
 */
public class ModifierAsmType implements AsmType {
  @Nullable
  private static ModifierAsmType INSTANCE;

  private ModifierAsmType() {
  }

  /**
   * Get the singleton instance of this AsmType.
   *
   * @return instance of the AsmType
   */
  public static AsmType instance() {
    if (INSTANCE == null) {
      INSTANCE = new ModifierAsmType();
    }
    return INSTANCE;
  }

  @Override
  public String name() {
    return "modifier";
  }

  @Override
  public String toCppTypeString(String prefix) {
    return prefix + "MCExpr::VariantKind";
  }

  @Override
  public boolean canBeCastTo(AsmType to) {
    return to == this || to == VoidAsmType.instance();
  }

  @Override
  public String toString() {
    return "@" + name();
  }
}
