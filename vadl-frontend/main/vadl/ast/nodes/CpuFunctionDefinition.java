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

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class CpuFunctionDefinition extends Definition implements IdentifiableNode {
  public Identifier id;
  public BehaviorKind kind;
  @Nullable
  public IsId stopWithReference;
  public Expr expr;
  public SourceLocation loc;

  public CpuFunctionDefinition(Identifier id, BehaviorKind kind, @Nullable IsId stopWithReference,
                        Expr expr,
                        SourceLocation loc) {
    this.id = id;
    this.kind = kind;
    this.stopWithReference = stopWithReference;
    this.expr = expr;
    this.loc = loc;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append(kind.keyword);
    if (stopWithReference != null) {
      builder.append(" with @");
      stopWithReference.prettyPrint(0, builder);
    }
    if (isBlockLayout(expr)) {
      builder.append(" =\n");
      expr.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" = ");
      expr.prettyPrint(0, builder);
      builder.append("\n");
    }
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    if (stopWithReference != null) {
      action.accept((Node) stopWithReference);
    }

    action.accept(expr);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CpuFunctionDefinition that = (CpuFunctionDefinition) o;
    return kind == that.kind && Objects.equals(stopWithReference, that.stopWithReference)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, stopWithReference, expr);
  }

  @Override
  public Identifier identifier() {
    return id;
  }

  public enum BehaviorKind {
    STOP("stop");

    public final String keyword;

    BehaviorKind(String keyword) {
      this.keyword = keyword;
    }
  }
}
