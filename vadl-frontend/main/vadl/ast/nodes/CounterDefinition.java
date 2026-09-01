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

import java.util.Locale;
import java.util.function.Consumer;
import vadl.types.Type;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class CounterDefinition extends Definition implements IdentifiableNode, TypedNode {
  public CounterKind kind;
  public IdentifierOrPlaceholder identifier;
  public TypeLiteral typeLiteral;
  public SourceLocation loc;

  @Override
  public Type type() {
    return typeLiteral.type();
  }

  public enum CounterKind {
    PROGRAM,
    GROUP
  }

  public CounterDefinition(CounterKind kind, IdentifierOrPlaceholder identifier,
                           TypeLiteral typeLiteral,
                           SourceLocation location) {
    this.kind = kind;
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.loc = location;
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
    builder.append("%s counter ".formatted(kind.toString().toLowerCase(Locale.ENGLISH)));
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    typeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(typeLiteral);
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s kind: %s".formatted(this.getClass().getSimpleName(), kind.toString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CounterDefinition that = (CounterDefinition) o;
    return annotations.equals(that.annotations)
        && kind == that.kind
        && identifier.equals(that.identifier)
        && typeLiteral.equals(that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + kind.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }
}
