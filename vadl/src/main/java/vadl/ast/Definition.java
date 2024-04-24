package vadl.ast;

import java.util.List;
import java.util.Objects;

abstract class Definition extends Node {
}

class InstructionSetDefinition extends Definition {
  final Identifier identifier;
  final Location loc;
  List<Stmt> statements;

  InstructionSetDefinition(Identifier identifier, List<Stmt> statements, Location location) {
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
    builder.append(indentString(indent));
    builder.append("InstructionSetDefinition \"%s\"\n".formatted(identifier.name));

    for (Stmt stmt : statements) {
      stmt.dump(indent + 1, builder);
    }
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("instruction set architecture %s = {\n".formatted(identifier.name));
    for (Stmt stmt : statements) {
      stmt.prettyPrint(indent + 1, builder);
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

    InstructionSetDefinition that = (InstructionSetDefinition) o;
    return Objects.equals(identifier, that.identifier) &&
        Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(statements);
    return result;
  }
}
