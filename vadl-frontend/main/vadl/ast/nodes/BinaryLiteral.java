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

package vadl.ast.nodes;

import java.math.BigInteger;
import java.util.Objects;
import vadl.utils.SourceLocation;

/**
 * A hex or binary integer literal.
 * Example: `0xaf` for hex or `0b01` for binary.
 */
@SuppressWarnings("MissingJavadocMethod")
public class BinaryLiteral extends Expr {
  public String token;
  public BigInteger number;
  public int bitWidth;
  public SourceLocation loc;

  public BinaryLiteral(String token, SourceLocation loc) {
    this.token = token;
    this.loc = loc;

    var simplifiedToken = token.replace("'", "");
    if (token.startsWith("0x")) {
      this.number = new BigInteger(simplifiedToken.substring(2), 16);
      this.bitWidth = (simplifiedToken.length() - 2) * 4;
    } else if (simplifiedToken.startsWith("0b")) {
      this.number = new BigInteger(simplifiedToken.substring(2), 2);
      this.bitWidth = (simplifiedToken.length() - 2);
    } else {
      throw new IllegalArgumentException("No conversion implemented for binary literal " + token);
    }
  }

  private BinaryLiteral(String token, BigInteger number, int bitWidth, SourceLocation location) {
    this.token = token;
    this.loc = location;
    this.number = number;
    this.bitWidth = bitWidth;
  }

  public BinaryLiteral copyWithLocation(SourceLocation location) {
    return new BinaryLiteral(this.token, this.number, this.bitWidth, location);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.BIN;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(token);
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
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

    BinaryLiteral that = (BinaryLiteral) o;
    return number.equals(that.number) && token.equals(that.token);
  }

  @Override
  public int hashCode() {
    int result = number.hashCode();
    result = 31 * result + Objects.hashCode(token);
    return result;
  }
}
