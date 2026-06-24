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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

/**
 * An identifier path pointing to one or multiple nested namespaces, separated by {@code ::}.
 * The first N-1 segments are namespace references and the last segment is the actual identifier.
 *
 * <p>Example: {@code MyIsa::MyFormat::fieldName}
 */
@SuppressWarnings("MissingJavadocMethod")
public final class IdentifierPath extends Expr implements IsId {
  /**
   * List of segments in this path; the first N-1 segments are (nested) namespaces,
   * the last segment is an identifier in the (nested) namespace.
   * Size has to be at least 1
   */
  public List<IdentifierOrPlaceholder> segments;

  /**
   * The node this identifier refers to.
   */
  @Nullable
  public Node target;

  public IdentifierPath(List<IdentifierOrPlaceholder> segments) {
    this.segments = segments;
  }

  @Override
  public SourceLocation location() {
    var first = (Node) segments.get(0);
    var last = (Node) segments.get(segments.size() - 1);
    return first.location().join(last.location());
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.ID;
  }

  @Override
  public String pathToString() {
    var builder = new StringJoiner("::");
    for (var segment : segments) {
      builder.add(((Identifier) segment).name);
    }
    return builder.toString();
  }

  @Nullable
  @Override
  public Node target() {
    return target;
  }

  public String lastSegmentName() {
    return ((Identifier) segments.getLast()).name;
  }

  //  @Override
  public List<String> pathToSegments() {
    var result = new ArrayList<String>(segments.size());
    for (var segment : segments) {
      result.add(((Identifier) segment).name);
    }
    return result;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    var isFirst = true;
    for (var segment : segments) {
      if (!isFirst) {
        builder.append("::");
      }
      isFirst = false;
      ((Node) segment).prettyPrint(indent, builder);
    }
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s name: \"%s\"".formatted(this.getClass().getSimpleName(), pathToString());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IdentifierPath that = (IdentifierPath) o;
    return segments.equals(that.segments);
  }

  @Override
  public int hashCode() {
    return segments.hashCode();
  }
}
