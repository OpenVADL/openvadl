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

import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class WildcardLiteral extends Expr {
  public SourceLocation loc;

  public WildcardLiteral(SourceLocation loc) {
    this.loc = loc;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("*");
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.SYM_EX;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public final boolean equals(Object o) {
    return o instanceof WildcardLiteral;
  }

  @Override
  public int hashCode() {
    return "*".hashCode();
  }
}
