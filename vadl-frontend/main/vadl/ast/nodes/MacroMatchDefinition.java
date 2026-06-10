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

/**
 * An internal temporary placeholder of a macro-level "match" construct.
 * This node should never leave the parser.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class MacroMatchDefinition extends Definition implements IsMacroMatch {
  public MacroMatch macroMatch;

  public MacroMatchDefinition(MacroMatch macroMatch) {
    this.macroMatch = macroMatch;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return macroMatch.sourceLocation();
  }

  @Override
  public SyntaxType syntaxType() {
    return macroMatch.resultType();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    macroMatch.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroMatchDefinition that = (MacroMatchDefinition) o;
    return macroMatch.equals(that.macroMatch);
  }

  @Override
  public int hashCode() {
    return macroMatch.hashCode();
  }
}
