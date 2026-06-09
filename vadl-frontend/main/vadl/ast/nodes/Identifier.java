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
 * A reference to a named entity in the specification.
 * The {@link #target} field is resolved during symbol resolving to point to the
 * referenced {@link Node}.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class Identifier extends Expr implements IsId, IdentifierOrPlaceholder {
  public String name;
  public SourceLocation loc;

  /**
   * The node this identifier refers to.
   */
  @Nullable
  public Node target;

  public Identifier(String name, SourceLocation location) {
    this.loc = location;
    this.name = name;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.ID;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(name);
  }

  @Override
  public String pathToString() {
    return name;
  }

  @Nullable
  @Override
  public Node target() {
    return target;
  }

  @Override
  public String toString() {
    return "%s name: \"%s\"".formatted(this.getClass().getSimpleName(), this.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Identifier that = (Identifier) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
