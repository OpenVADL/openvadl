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
import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class ExistsInExpr extends Expr {

  SourceLocation loc;

  @Child
  List<IsId> operations;

  ExistsInExpr(List<IsId> operations, SourceLocation loc) {
    this.operations = operations;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("exists in {");
    var isFirst = true;
    for (IsId operation : operations) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      operation.prettyPrint(0, builder);
    }
    builder.append("}");
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
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
    ExistsInExpr that = (ExistsInExpr) o;
    return Objects.equals(operations, that.operations);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(operations);
  }
}
