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

import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * A representation of terms of form {@code "MEM<9>"}.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class SymbolExpr extends Expr implements IsSymExpr {
  @Child
  public IsId path;
  @Child
  public Expr size;
  public SourceLocation location;

  public SymbolExpr(IsId path, Expr size, SourceLocation location) {
    this.path = path;
    this.size = size;
    this.location = location;
  }

  @Override
  public IsId path() {
    return path;
  }

  @Override
  public Expr size() {
    return size;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.SYM_EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    path.prettyPrint(indent, builder);
    var prefix = size instanceof BinaryExpr ? "<( " : "< ";
    var suffix = size instanceof BinaryExpr ? " )>" : " >";
    builder.append(prefix);
    size.prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
    builder.append(suffix);
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SymbolExpr that = (SymbolExpr) o;
    return path.equals(that.path) && Objects.equals(size, that.size);
  }

  @Override
  public int hashCode() {
    int result = path.hashCode();
    result = 31 * result + Objects.hashCode(size);
    return result;
  }
}
