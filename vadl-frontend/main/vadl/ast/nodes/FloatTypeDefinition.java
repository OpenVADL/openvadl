// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.types.FloatEncoding;
import vadl.types.FloatType;
import vadl.types.Type;
import vadl.utils.SourceLocation;

/**
 * Represents a float-type definition, which is used to specify float types.
 * <pre>{@code
 * [ IEEE : 32 ]
 * float-type Binary32
 * }</pre>
 * Float types represent {@link vadl.viam.FloatFormat}s.
 */
public class FloatTypeDefinition extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder identifier;

  /**
   * Represents the encoding of the represented float format. This is used by the type-checker
   * to enforce certain rules for float built-ins (e.g. that the fcvt built-in cannot convert
   * between two float types with the same encoding).
   * This is not set during parsing, and must be set by an annotation such as {@code [ IEEE : 32 ]}.
   */
  @Nullable
  public FloatEncoding encoding;

  public SourceLocation loc;

  @Nullable
  private FloatType type;

  public FloatTypeDefinition(IdentifierOrPlaceholder identifier, SourceLocation loc) {
    this.identifier = identifier;
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
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("float-type %s".formatted(identifier().name));
    builder.append("\n");
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FloatTypeDefinition that = (FloatTypeDefinition) o;
    return Objects.equals(identifier, that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(identifier);
  }

  @Override
  public Type type() {
    if (type == null) {
      // Note: the definition must be type-checked beforehand
      type = new FloatType(requireNonNull(encoding));
    }
    return type;
  }
}
