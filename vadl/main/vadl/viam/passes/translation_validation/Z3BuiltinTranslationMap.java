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

package vadl.viam.passes.translation_validation;

import static vadl.viam.passes.translation_validation.Z3BuiltinTranslationMap.OperationsMode.INFIX;
import static vadl.viam.passes.translation_validation.Z3BuiltinTranslationMap.OperationsMode.PREFIX;

import java.util.Objects;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.utils.Pair;
import vadl.viam.passes.translation_validation.TranslationValidation.Z3Code;

/**
 * {@link BuiltIn} require translation to Z3 methods.
 */
public class Z3BuiltinTranslationMap {
  /**
   * Indicates how the operation should be used.
   * There are infix operations like +,- etc.
   * And prefix like AND, OR etc.
   */
  public enum OperationsMode {
    INFIX,
    PREFIX
  }

  /**
   * Translates the {@link BuiltIn} to a Z3 operation.
   * The {@link OperationsMode} defines how the operation should be used.
   */
  public static Pair<Z3Code, OperationsMode> lower(BuiltIn builtin) {
    if (builtin == BuiltInTable.EQU) {
      return new Pair<>(new Z3Code("=="), INFIX);
    } else if (builtin == BuiltInTable.SLTH) {
      return new Pair<>(new Z3Code("<"), INFIX);
    } else if (builtin == BuiltInTable.ULTH) {
      return new Pair<>(new Z3Code("ULT"), PREFIX);
    } else if (builtin == BuiltInTable.SLEQ) {
      return new Pair<>(new Z3Code("<="), INFIX);
    } else if (builtin == BuiltInTable.ULEQ) {
      return new Pair<>(new Z3Code("ULE"), PREFIX);
    } else if (builtin == BuiltInTable.SGTH) {
      return new Pair<>(new Z3Code(">"), INFIX);
    } else if (builtin == BuiltInTable.UGTH) {
      return new Pair<>(new Z3Code("UGT"), PREFIX);
    } else if (builtin == BuiltInTable.SGEQ) {
      return new Pair<>(new Z3Code(">="), INFIX);
    } else if (builtin == BuiltInTable.UGEQ) {
      return new Pair<>(new Z3Code("UGE"), PREFIX);
    } else if (builtin == BuiltInTable.UMOD) {
      return new Pair<>(new Z3Code("URem"), PREFIX);
    } else if (builtin == BuiltInTable.LSL) {
      return new Pair<>(new Z3Code("<<"), INFIX);
    } else if (builtin == BuiltInTable.LSR) {
      return new Pair<>(new Z3Code("LShR"), PREFIX);
    } else if (builtin == BuiltInTable.ASR) {
      return new Pair<>(new Z3Code(">>"), INFIX);
    } else if (builtin == BuiltInTable.UDIV) {
      return new Pair<>(new Z3Code("UDiv"), PREFIX);
    } else if (builtin == BuiltInTable.NEG) {
      return new Pair<>(new Z3Code("~"), PREFIX);
    } else if (builtin == BuiltInTable.SMULL
        || builtin == BuiltInTable.UMULL
        || builtin == BuiltInTable.SUMULL) {
      return new Pair<>(new Z3Code("*"), INFIX);
    } else {
      return new Pair<>(new Z3Code(Objects.requireNonNull(builtin.operator())), INFIX);
    }
  }
}
