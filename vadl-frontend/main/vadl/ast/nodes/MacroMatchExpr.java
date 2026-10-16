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

import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

/**
 * An internal temporary placeholder of a macro-level "match" construct.
 * This node should never leave the parser.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class MacroMatchExpr extends Expr
    implements MacroMatch, IdentifierOrMacro, IdentifierPathOrMacro {
  public MacroMatchContent macroMatchContent;

  public MacroMatchExpr(MacroMatchContent macroMatchContent) {
    this.macroMatchContent = macroMatchContent;
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return macroMatchContent.sourceLocation();
  }

  @Override
  public SyntaxType syntaxType() {
    return macroMatchContent.resultType();
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    macroMatchContent.prettyPrint(indent, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroMatchExpr that = (MacroMatchExpr) o;
    return macroMatchContent.equals(that.macroMatchContent);
  }

  @Override
  public int hashCode() {
    return macroMatchContent.hashCode();
  }

  @Override
  public String pathToString() {
    return "/* Match - can't be rendered! */";
  }

  @Nullable
  @Override
  public Node target() {
    return null;
  }
}
