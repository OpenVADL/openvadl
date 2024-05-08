package vadl.ast;

import java.util.List;
import java.util.Objects;

/**
 * A Section is the top level construct of the AST.
 */
abstract class Section extends Node {
}

class CommonDefinitionSection extends Section {
  Definition statement;

  CommonDefinitionSection(Definition statement) {
    this.statement = statement;
  }

  @Override
  Location location() {
    return statement.location();
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    statement.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    statement.prettyPrint(indent, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CommonDefinitionSection that = (CommonDefinitionSection) o;
    return statement.equals(that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(statement);
  }
}

class InstructionSetSection extends Section {
  final Identifier identifier;
  final Location loc;
  List<Definition> statements;

  InstructionSetSection(Identifier identifier, List<Definition> statements, Location location) {
    this.identifier = identifier;
    this.statements = statements;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("%s \"%s\"\n".formatted(this.getClass().getSimpleName(), identifier.name));

    for (Definition definition : statements) {
      definition.dump(indent + 1, builder);
    }
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("instruction set architecture %s = {\n".formatted(identifier.name));
    for (Definition definition : statements) {
      definition.prettyPrint(indent + 1, builder);
    }
    builder.append("}\n\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InstructionSetSection that = (InstructionSetSection) o;
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(statements);
    return result;
  }
}
