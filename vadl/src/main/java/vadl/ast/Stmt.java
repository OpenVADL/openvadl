package vadl.ast;

import java.util.Objects;

/**
 * The Statement nodes inside the AST.
 */
abstract class Stmt extends Node {
}

class ConstantDefinitionStmt extends Stmt {
  final Identifier identifier;
  final Expr value;
  final Location loc;

  ConstantDefinitionStmt(Identifier identifier, Expr value, Location location) {
    this.identifier = identifier;
    this.value = value;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("ConstantDefinitionStmt\n");
    identifier.dump(indent + 1, builder);
    value.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("constant %s = ".formatted(identifier.name));
    value.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ConstantDefinitionStmt that = (ConstantDefinitionStmt) o;
    return Objects.equals(identifier, that.identifier) &&
        Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }
}