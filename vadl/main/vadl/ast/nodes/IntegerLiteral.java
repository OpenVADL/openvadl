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

import java.math.BigInteger;
import java.util.Objects;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class IntegerLiteral extends Expr {
  String token;
  BigInteger number;
  SourceLocation loc;

  private static BigInteger parse(String token) {
    return new BigInteger(token.replace("'", ""));
  }

  public IntegerLiteral(String token, SourceLocation loc) {
    this.token = token;
    this.number = parse(token);
    this.loc = loc;
  }

  // An alternative constructor for when the number is already known.
  // Mostly used for macro expanding.
  private IntegerLiteral(String token, BigInteger number, SourceLocation loc) {
    this.token = token;
    this.number = number;
    this.loc = loc;
  }

  // An internal constructor for when we want to create an integer literal synthetically
  public IntegerLiteral(int number, SourceLocation loc) {
    this.token = Integer.toString(number);
    this.number = BigInteger.valueOf(number);
    this.loc = loc;
  }

  public IntegerLiteral copyWithLocation(SourceLocation location) {
    return new IntegerLiteral(this.token, this.number, location);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INT;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(token);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s literal: %s (%d)".formatted(this.getClass().getSimpleName(), token, number);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IntegerLiteral that = (IntegerLiteral) o;
    return number.equals(that.number) && token.equals(that.token);
  }

  @Override
  public int hashCode() {
    int result = number.hashCode();
    result = 31 * result + Objects.hashCode(token);
    return result;
  }
}
