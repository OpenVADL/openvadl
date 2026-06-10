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

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.ConcreteRelationType;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class MemoryDefinition extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder identifier;
  @Child
  public TypeLiteral addressTypeLiteral;
  @Child
  public TypeLiteral dataTypeLiteral;
  public SourceLocation loc;

  @Nullable
  public ConcreteRelationType type;

  public MemoryDefinition(IdentifierOrPlaceholder identifier, TypeLiteral addressTypeLiteral,
                          TypeLiteral dataTypeLiteral, SourceLocation loc) {
    this.identifier = identifier;
    this.addressTypeLiteral = addressTypeLiteral;
    this.dataTypeLiteral = dataTypeLiteral;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("memory ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    addressTypeLiteral.prettyPrint(indent, builder);
    builder.append(" -> ");
    dataTypeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
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

    MemoryDefinition that = (MemoryDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && addressTypeLiteral.equals(that.addressTypeLiteral)
        && dataTypeLiteral.equals(that.dataTypeLiteral);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + addressTypeLiteral.hashCode();
    result = 31 * result + dataTypeLiteral.hashCode();
    return result;
  }

  @Override
  public ConcreteRelationType type() {
    return requireNonNull(type);
  }
}
