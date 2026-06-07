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
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public sealed interface Group {

  void prettyPrint(int indent, StringBuilder builder);

  <R> R accept(GroupVisitor<R> visitor);

  final class Sequence extends Node implements Group {

    @Child
    List<Group> groups;
    SourceLocation loc;

    Sequence(List<Group> groups, SourceLocation loc) {
      this.groups = groups;
      this.loc = loc;
    }

    @Override
    public SourceLocation location() {
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      var isFirst = true;
      for (var group : groups) {
        if (!isFirst) {
          builder.append(".");
        }
        isFirst = false;
        group.prettyPrint(indent, builder);
      }
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
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
      Sequence sequence = (Sequence) o;
      return Objects.equals(groups, sequence.groups);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(groups);
    }
  }

  final class Literal extends Node implements Group {

    @Child
    IsId id;
    @Child
    @Nullable
    Expr size;
    SourceLocation loc;

    /**
     * Operation literal matched by this literal, initialized during type checking.
     */
    @LazyInit
    OperationDefinition operation;

    Literal(IsId id, @Nullable Expr size, SourceLocation loc) {
      this.id = id;
      this.size = size;
      this.loc = loc;
    }

    public OperationDefinition getOperation() {
      return operation;
    }

    public void setOperation(OperationDefinition operation) {
      this.operation = operation;
    }

    @Override
    public SourceLocation location() {
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      id.prettyPrint(indent, builder);
      if (size != null) {
        builder.append("<");
        size.prettyPrint(indent, builder);
        builder.append(">");
      }
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
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
      Literal literal = (Literal) o;
      return Objects.equals(id, literal.id) && Objects.equals(size, literal.size);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, size);
    }
  }

  final class Alternative extends Node implements Group {

    @Child
    List<Sequence> sequences;
    SourceLocation loc;

    Alternative(List<Sequence> sequences, SourceLocation loc) {
      this.sequences = sequences;
      this.loc = loc;
    }

    @Override
    public SourceLocation location() {
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      builder.append("(");
      var isFirst = true;
      for (var sequence : sequences) {
        if (!isFirst) {
          builder.append(" | ");
        }
        isFirst = false;
        sequence.prettyPrint(indent, builder);
      }
      builder.append(")");
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
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
      Alternative that = (Alternative) o;
      return Objects.equals(sequences, that.sequences);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(sequences);
    }
  }

  final class Permutation extends Node implements Group {

    @Child
    List<Sequence> sequences;
    SourceLocation loc;

    Permutation(List<Sequence> sequences, SourceLocation loc) {
      this.sequences = sequences;
      this.loc = loc;
    }

    @Override
    public SourceLocation location() {
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      builder.append("{");
      var isFirst = true;
      for (var sequence : sequences) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        sequence.prettyPrint(indent, builder);
      }
      builder.append("}");
    }

    @Override
    public <R> R accept(GroupVisitor<R> visitor) {
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
      Permutation that = (Permutation) o;
      return Objects.equals(sequences, that.sequences);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(sequences);
    }
  }

  interface GroupVisitor<R> {
    R visit(Sequence seq);

    R visit(Alternative alt);

    R visit(Permutation perm);

    R visit(Literal lit);
  }
}
