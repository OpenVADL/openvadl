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

import vadl.javaannotations.ast.Child;
import vadl.types.Type;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class StageOutputDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder identifier;
  @Child
  TypeLiteral typeLiteral;

  public StageOutputDefinition(IdentifierOrPlaceholder identifier, TypeLiteral typeLiteral) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
    builder.append(" : ");
    typeLiteral.prettyPrint(indent, builder);
  }

  @Override
  public SourceLocation location() {
    return identifier.location().join(typeLiteral.location());
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StageOutputDefinition output = (StageOutputDefinition) o;
    return identifier.equals(output.identifier)
        && typeLiteral.equals(output.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }

  @Override
  public Type type() {
    return typeLiteral.type();
  }
}
