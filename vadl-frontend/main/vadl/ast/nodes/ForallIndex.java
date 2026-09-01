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

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

/**
 * An index variable binding in a {@link ForallStatement} or {@link ForallExpr}.
 * Binds a name to a range domain.
 * e.g. {@code i: Bits<8> in 0..3}.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class ForallIndex extends Node implements IdentifiableNode {
  public IsId name;

  @Nullable
  public TypeLiteral typeLiteral;

  public Expr domain;

  /**
   * Set by the typechecker.
   */
  @Nullable
  public Integer computedFrom;
  @Nullable
  public Integer computedTo;

  public ForallIndex(IsId name, @Nullable TypeLiteral typeLiteral, Expr domain) {
    this.name = name;
    this.typeLiteral = typeLiteral;
    this.domain = domain;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  public SourceLocation location() {
    return name.location().join(domain.location());
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    name.prettyPrint(0, builder);
    if (typeLiteral != null) {
      builder.append(": ");
      typeLiteral.prettyPrint(0, builder);
    }
    builder.append(" in ");
    domain.prettyPrint(0, builder);
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept((Node) name);

    if (typeLiteral != null) {
      action.accept(typeLiteral);
    }

    action.accept(domain);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ForallIndex index = (ForallIndex) o;
    return name.equals(index.name) && Objects.equals(typeLiteral, index.typeLiteral)
        && domain.equals(index.domain);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Objects.hashCode(typeLiteral);
    result = 31 * result + domain.hashCode();
    return result;
  }
}
