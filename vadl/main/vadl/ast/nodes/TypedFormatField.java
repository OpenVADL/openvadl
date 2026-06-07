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
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class TypedFormatField extends FormatField implements IdentifiableNode {
  @Child
  TypeLiteral typeLiteral;

  // The range this field occupies in its parent format.
  @Nullable
  FormatDefinition.BitRange range;

  public TypedFormatField(IdentifierOrPlaceholder identifier, TypeLiteral typeLiteral) {
    super(identifier);
    this.typeLiteral = typeLiteral;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  public SourceLocation location() {
    return identifier().location().join(typeLiteral.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
    builder.append(" : ");
    typeLiteral.prettyPrint(indent, builder);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TypedFormatField that = (TypedFormatField) o;
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(typeLiteral, that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(typeLiteral);
    result = 31 * result + Objects.hashCode(symbolTable);
    return result;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
