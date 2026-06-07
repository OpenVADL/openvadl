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

import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AsmModifierDefinition extends Definition {
  @Child
  Expr stringLiteral;
  @Child
  Identifier isa;
  @Child
  Identifier relocation;
  SourceLocation loc;

  public AsmModifierDefinition(Expr stringLiteral, Identifier isa, Identifier relocation,
                               SourceLocation loc) {
    this.stringLiteral = stringLiteral;
    this.isa = isa;
    this.relocation = relocation;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    stringLiteral.prettyPrint(0, builder);
    builder.append(" -> ");
    isa.prettyPrint(0, builder);
    builder.append("::");
    relocation.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmModifierDefinition that = (AsmModifierDefinition) o;
    return Objects.equals(stringLiteral, that.stringLiteral) && Objects.equals(isa, that.isa)
        && Objects.equals(relocation, that.relocation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringLiteral, isa, relocation);
  }
}
