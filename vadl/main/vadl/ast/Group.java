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
    SourceLocation location() {
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
    @Nullable Expr size;
    SourceLocation loc;

    Literal(IsId id, @Nullable Expr size, SourceLocation loc) {
      this.id = id;
      this.size = size;
      this.loc = loc;
    }

    @Override
    SourceLocation location() {
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
    SourceLocation location() {
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
    SourceLocation location() {
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
