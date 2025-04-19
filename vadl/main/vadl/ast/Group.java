// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

sealed interface Group {

  void prettyPrint(int indent, StringBuilder builder);

  class Sequence extends Node {

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
    void prettyPrint(int indent, StringBuilder builder) {
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

    IsId id;
    @Nullable
    Expr size;
    SourceLocation loc;

    Literal(IsId id, @Nullable Expr size, SourceLocation loc) {
      this.id = id;
      this.size = size;
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
      id.prettyPrint(indent, builder);
      if (size != null) {
        builder.append("<");
        size.prettyPrint(indent, builder);
        builder.append(">");
      }
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
}
