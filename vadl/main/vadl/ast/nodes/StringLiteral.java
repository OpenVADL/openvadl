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

import vadl.ast.StringLiteralParser;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class StringLiteral extends Expr {
  public String token;
  public String value;
  public SourceLocation loc;

  // FIXME: Cleanup StringLiteral constructors, each one does interpret their arguments a bit
  // differently, which is quite confusing to use.

  public StringLiteral(String token, SourceLocation loc) {
    this.token = token;
    if (token.length() > 1) {
      this.value = StringLiteralParser.parseString(token.substring(1, token.length() - 1));
    } else {
      this.value = token;
    }
    this.loc = loc;
  }

  public StringLiteral(String value) {
    this.token = '"' + value + '"';
    this.value = value;
    this.loc = SourceLocation.INVALID_SOURCE_LOCATION;
  }

  public StringLiteral(String token, String value, SourceLocation loc) {
    this.token = token;
    this.value = value;
    this.loc = loc;
  }

  public StringLiteral copyWithLocation(SourceLocation location) {
    return new StringLiteral(this.token, this.value, location);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.STR;
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
    return "%s literal: \"%s\" (%s)".formatted(this.getClass().getSimpleName(), value, token);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StringLiteral that = (StringLiteral) o;
    return token.equals(that.token);
  }

  @Override
  public int hashCode() {
    return token.hashCode();
  }
}
