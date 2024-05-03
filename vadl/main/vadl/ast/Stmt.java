package vadl.ast;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The Statement nodes inside the AST.
 */
abstract class Stmt extends Node {
}

class ConstantDefinitionStmt extends Stmt {
  final Identifier identifier;

  @Nullable
  final TypeLiteral typeAnnotation;

  final Expr value;
  final Location loc;

  ConstantDefinitionStmt(Identifier identifier, @Nullable TypeLiteral typeAnnotation, Expr value,
                         Location location) {
    this.identifier = identifier;
    this.typeAnnotation = typeAnnotation;
    this.value = value;
    this.loc = location;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("ConstantDefinitionStmt\n");
    if (typeAnnotation != null) {
      typeAnnotation.dump(indent + 1, builder);
    }
    identifier.dump(indent + 1, builder);
    value.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("constant %s".formatted(identifier.name));
    if (typeAnnotation != null) {
      builder.append(": ");
      typeAnnotation.prettyPrint(indent, builder);
    }
    builder.append(" = ");
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
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(typeAnnotation, that.typeAnnotation)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(typeAnnotation);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }
}

class MemoryDefinitionStmt extends Stmt {
  Identifier identifier;
  TypeLiteral addressType;
  TypeLiteral dataType;
  Location loc;

  public MemoryDefinitionStmt(Identifier identifier, TypeLiteral addressType, TypeLiteral dataType,
                              Location loc) {
    this.identifier = identifier;
    this.addressType = addressType;
    this.dataType = dataType;
    this.loc = loc;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append("MemoryDefinitionStmt\n");
    addressType.dump(indent + 1, builder);
    dataType.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("memory ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    addressType.prettyPrint(indent, builder);
    builder.append(" -> ");
    dataType.prettyPrint(indent, builder);
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

    MemoryDefinitionStmt that = (MemoryDefinitionStmt) o;
    return identifier.equals(that.identifier) && addressType.equals(that.addressType)
        && dataType.equals(that.dataType);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + addressType.hashCode();
    result = 31 * result + dataType.hashCode();
    return result;
  }
}
