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

import java.util.function.Consumer;
import vadl.utils.SourceLocation;

/**
 * A reference literal that references a structural element using the {@code @} prefix.
 * Written as {@code @identifier}, e.g. {@code @PC} to reference the program counter resource.
 * This syntax is mostly used in the microarchitecture to differentiate between direct usages and
 * references.
 */
@SuppressWarnings("MissingJavadocMethod")
public class ResourceReferenceExression extends Expr {
  public IdentifierOrPlaceholder resource;
  public SourceLocation location;

  public ResourceReferenceExression(IdentifierOrPlaceholder resource, SourceLocation location) {
    this.resource = resource;
    this.location = location;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("@");
    resource.prettyPrint(indent, builder);
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept((Node) resource);
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ResourceReferenceExression that = (ResourceReferenceExression) o;
    return resource.equals(that.resource);
  }

  @Override
  public int hashCode() {
    return resource.hashCode();
  }
}
