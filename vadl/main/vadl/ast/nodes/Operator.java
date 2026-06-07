// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import javax.annotation.Nullable;

@SuppressWarnings("MissingJavadocType")
public class Operator {
  final String symbol;
  final Precedence precedence;

  private Operator(String symbol, Precedence precedence) {
    this.symbol = symbol;
    this.precedence = precedence;
  }

  @Override
  public String toString() {
    return symbol;
  }

  public static final Operator LogicalOr = new Operator("||", Precedence.LogicalOr);
  public static final Operator LogicalAnd = new Operator("&&", Precedence.LogicalAnd);
  public static final Operator Or = new Operator("|", Precedence.Or);
  public static final Operator Xor = new Operator("^", Precedence.Xor);
  public static final Operator And = new Operator("&", Precedence.And);
  public static final Operator Equal = new Operator("=", Precedence.Equality);
  public static final Operator NotEqual = new Operator("!=", Precedence.Equality);
  public static final Operator GreaterEqual = new Operator(">=", Precedence.Comparison);
  public static final Operator Greater = new Operator(">", Precedence.Comparison);
  public static final Operator LessEqual = new Operator("<=", Precedence.Comparison);
  public static final Operator Less = new Operator("<", Precedence.Comparison);
  public static final Operator RotateRight = new Operator("<>>", Precedence.Shift);
  public static final Operator RotateLeft = new Operator("<<>", Precedence.Shift);
  public static final Operator ShiftRight = new Operator(">>", Precedence.Shift);
  public static final Operator ShiftLeft = new Operator("<<", Precedence.Shift);
  public static final Operator Add = new Operator("+", Precedence.Term);
  public static final Operator Subtract = new Operator("-", Precedence.Term);
  public static final Operator SaturatedAdd = new Operator("+|", Precedence.Term);
  public static final Operator SaturatedSubtract = new Operator("-|", Precedence.Term);
  public static final Operator Multiply = new Operator("*", Precedence.Factor);
  public static final Operator Divide = new Operator("/", Precedence.Factor);
  public static final Operator Modulo = new Operator("%", Precedence.Factor);
  public static final Operator LongMultiply = new Operator("*#", Precedence.Factor);
  public static final Operator In = new Operator("in", Precedence.In);
  public static final Operator NotIn = new Operator("!in", Precedence.In);
  public static final Operator ElementOf = new Operator("∈", Precedence.In);
  public static final Operator NotElementOf = new Operator("∉", Precedence.In);

  public static final List<Operator> allOperators = List.of(
      LogicalOr,
      LogicalAnd,
      Or,
      Xor,
      And,
      Equal,
      NotEqual,
      GreaterEqual,
      Greater,
      LessEqual,
      Less,
      RotateRight,
      RotateLeft,
      ShiftRight,
      ShiftLeft,
      Add,
      Subtract,
      SaturatedAdd,
      SaturatedSubtract,
      Multiply,
      Divide,
      Modulo,
      LongMultiply,
      In,
      NotIn,
      ElementOf,
      NotElementOf
  );
  public static final List<Operator> logicalComparisions = List.of(LogicalOr, LogicalAnd);
  public static final List<Operator> arithmeticOperators =
      List.of(Or, Xor, And, RotateRight, RotateLeft, ShiftLeft, ShiftRight, Add, Subtract, Multiply,
          Divide, Modulo, LongMultiply);
  public static final List<Operator> artihmeticComparisons =
      List.of(Equal, NotEqual, GreaterEqual, Greater, LessEqual, Less
      );

  @Nullable
  public static Operator fromString(String operator) {
    return allOperators.stream().filter(op -> op.symbol.equals(operator)).findFirst().orElse(null);
  }
}
