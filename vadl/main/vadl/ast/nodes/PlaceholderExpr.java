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
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public final class PlaceholderExpr extends Expr implements IdentifierOrPlaceholder, IsId {
  List<String> segments;
  SyntaxType syntaxType;
  SourceLocation loc;

  public PlaceholderExpr(List<String> segments, SyntaxType syntaxType, SourceLocation loc) {
    this.segments = segments;
    this.syntaxType = syntaxType;
    this.loc = loc;
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return syntaxType;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("$");
    builder.append(String.join(".", segments));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PlaceholderExpr that = (PlaceholderExpr) o;
    return segments.equals(that.segments);
  }

  @Override
  public int hashCode() {
    return segments.hashCode();
  }

  @Override
  public String pathToString() {
    var sb = new StringBuilder();
    prettyPrint(0, sb);
    return sb.toString();
  }

  @Nullable
  @Override
  public Node target() {
    return null;
  }
}
