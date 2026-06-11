// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.ast;

import java.util.List;
import java.util.stream.Stream;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.Type;

/**
 * Frontend-only built-ins that depend on AST-specific type classes.
 */
final class FrontendBuiltIns {

  /**
   * Operation equality.
   *
   * <p>{@code function opequ ( a : PseudoFormatType, b : PseudoFormatType ) -> Bool // <=> a = b }
   */
  static final BuiltInTable.BuiltIn OP_EQU =
      BuiltInTable.func("VADL::opequ", "=",
          Type.relation(PseudoFormatType.class, PseudoFormatType.class, BoolType.class))
          .takesDefault()
          .returns(Type.bool())
          .build();

  /**
   * Operation inequality.
   *
   * <p>{@code function opneq ( a : PseudoFormatType, b : PseudoFormatType ) -> Bool // <=> a != b }
   */
  static final BuiltInTable.BuiltIn OP_NEQ =
      BuiltInTable.func("VADL::opneq", "!=",
          Type.relation(PseudoFormatType.class, PseudoFormatType.class, BoolType.class))
          .takesDefault()
          .returns(Type.bool())
          .build();

  static final List<BuiltInTable.BuiltIn> operationEqualityPredicates = List.of(OP_EQU, OP_NEQ);

  private FrontendBuiltIns() {}

  static Stream<BuiltInTable.BuiltIn> builtIns() {
    return Stream.concat(BuiltInTable.builtIns(), operationEqualityPredicates.stream());
  }
}
