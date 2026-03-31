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

import java.util.HashMap;
import java.util.Map;
import vadl.types.Type;

/**
 * An AsmType is used to type assembly grammar elements in the assembly description.
 *
 * <p>The following types are allowed to be used in the assembly grammar:</p>
 * <ul>
 *   <li>constant</li>
 *   <li>expression</li>
 *   <li>instruction</li>
 *   <li>modifier</li>
 *   <li>operand</li>
 *   <li>register</li>
 *   <li>string</li>
 *   <li>symbol</li>
 *   <li>void</li>
 *   <li>statements</li>
 *   <li>instructions</li>
 *   <li>operands</li>
 * </ul>
 *
 * <p>There is also a special GroupAsmType, used to type sequences of grammar elements.</p>
 */
public interface AsmType {
  /**
   * Mapping of AsmType names to their corresponding instances.
   *
   * @see AsmType
   */
  HashMap<String, AsmType> ASM_TYPES = new HashMap<>(Map.ofEntries(
      Map.entry(ConstantAsmType.instance().name(), ConstantAsmType.instance()),
      Map.entry(ExpressionAsmType.instance().name(), ExpressionAsmType.instance()),
      Map.entry(InstructionAsmType.instance().name(), InstructionAsmType.instance()),
      Map.entry(ModifierAsmType.instance().name(), ModifierAsmType.instance()),
      Map.entry(OperandAsmType.instance().name(), OperandAsmType.instance()),
      Map.entry(RegisterAsmType.instance().name(), RegisterAsmType.instance()),
      Map.entry(StringAsmType.instance().name(), StringAsmType.instance()),
      Map.entry(SymbolAsmType.instance().name(), SymbolAsmType.instance()),
      Map.entry(VoidAsmType.instance().name(), VoidAsmType.instance()),
      Map.entry(StatementsAsmType.instance().name(), StatementsAsmType.instance()),
      Map.entry(OperandsAsmType.instance().name(), OperandsAsmType.instance())
  ));

  /**
   * Check if the input string is a valid assembly grammar type.
   * The input string is valid if it is equal to the lowercase string representation
   * of any assembly grammar type.
   *
   * @param input the input string to check
   * @return true if the input string is a valid assembly type, false otherwise
   */
  static boolean isInputAsmType(String input) {
    return ASM_TYPES.containsKey(input);
  }

  /**
   * Get the AsmType corresponding to the given VADL type.
   *
   * @param operationalType the VADL type to get the AsmType for
   * @return the corresponding VADL Type
   * @throws UnsupportedOperationException if there is no AsmType for the given VADL type
   */
  static AsmType getAsmTypeFromOperationalType(Type operationalType) {
    if (operationalType == Type.signedInt(64)) {
      return ConstantAsmType.instance();
    } else if (operationalType == Type.string()) {
      return StringAsmType.instance();
    } else if (operationalType == Type.void_()) {
      return VoidAsmType.instance();
    } else {
      throw new UnsupportedOperationException("There is no VADL type for this AsmType.");
    }
  }

  /**
   * Check whether this AsmType can be cast to the given AsmType.
   *
   * @param to AsmType to be cast to
   * @return whether this AsmType can be cast to the given AsmType
   */
  boolean canBeCastTo(AsmType to);

  /**
   * Get the name of this AsmType.
   *
   * @return name of this AsmType
   */
  String name();

  /**
   * Get the corresponding type of the VADL type system.
   *
   * @return the corresponding VADL type
   * @throws UnsupportedOperationException if there is no VADL type for this AsmType
   */
  default Type toOperationalType() {
    throw new UnsupportedOperationException(
        "This AsmType does not have an corresponding VADL type.");
  }

  /**
   * Get the string representation of the corresponding CPP type.
   *
   * @param prefix prefix which might be added to the CPP type string
   * @return the corresponding CPP type string
   * @throws UnsupportedOperationException if there is no CPP type for this AsmType
   */
  default String toCppTypeString(String prefix) {
    throw new UnsupportedOperationException(
        "This AsmType does not have an corresponding CPP type.");
  }
}