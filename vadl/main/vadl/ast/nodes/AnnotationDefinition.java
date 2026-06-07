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

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public final class AnnotationDefinition extends Definition {

  List<IdentifierOrPlaceholder> keywords;

  @Child
  List<Expr> values;

  /**
   * The Definition on which it is defined.
   * Set by the parser.
   */
  @LazyInit
  Definition target;

  /**
   * Set by the symboltable.
   */
  @Nullable
  Annotation annotation;

  SourceLocation loc;

  public AnnotationDefinition(List<IdentifierOrPlaceholder> keywords, List<Expr> values,
                              SourceLocation loc) {
    this.keywords = keywords;
    this.values = values;
    this.loc = loc;
  }

  String name() {
    return keywords.stream()
        .map(i -> ((Identifier) i).name)
        .collect(Collectors.joining(" "));
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(Node.prettyIndentString(indent));
    builder.append("[ ");
    prettyPrintJoin(" ", keywords.stream().map(k -> (Node) k).toList(), indent, builder);

    if (!values.isEmpty()) {
      builder.append(" : ");
      prettyPrintJoin(", ", values, indent, builder);
    }
    builder.append(" ]\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AnnotationDefinition that = (AnnotationDefinition) o;
    return keywords.equals(that.keywords) && Objects.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    int result = keywords.hashCode();
    result = 31 * result + Objects.hashCode(values);
    return result;
  }
}
