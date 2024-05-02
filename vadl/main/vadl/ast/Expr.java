package vadl.ast;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The Expression node of the AST.
 */
public abstract class Expr extends Node {
}

class IntegerLiteral extends Expr {
  long number;
  Location loc;

  public IntegerLiteral(long number, Location loc) {
    this.number = number;
    this.loc = loc;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("IntegerLiteral (value: %d)\n".formatted(number));
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(number);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IntegerLiteral that = (IntegerLiteral) o;
    return number == that.number;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(number);
  }
}

/**
 * TypeLiterals are needed as the types are not known during parsing.
 * For example {@code Bits<counter>} depends on the constant {@code counter} used here and so some
 * constant evaluation has to be performed for the concrete type to be known here.
 */
class TypeLiteral extends Expr {
  Identifier baseType;

  @Nullable
  Expr sizeExpression;

  Location loc;

  public TypeLiteral(Identifier baseType, @Nullable Expr sizeExpression, Location loc) {
    this.baseType = baseType;
    this.sizeExpression = sizeExpression;
    this.loc = loc;
  }

  @Override
  Location location() {
    return loc;
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("TypeLiteral\n");
    baseType.dump(indent + 1, builder);
    if (sizeExpression != null) {
      sizeExpression.dump(indent + 1, builder);
    }
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(baseType.name);
    if (sizeExpression != null) {
      builder.append("<");
      sizeExpression.prettyPrint(
          indent, builder
      );
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

    TypeLiteral that = (TypeLiteral) o;
    return baseType.equals(that.baseType)
        && Objects.equals(sizeExpression, that.sizeExpression);
  }

  @Override
  public int hashCode() {
    int result = baseType.hashCode();
    result = 31 * result + Objects.hashCode(sizeExpression);
    return result;
  }
}

class Variable extends Expr {
  Identifier identifier;

  public Variable(Identifier identifier) {
    this.identifier = identifier;
  }

  @Override
  Location location() {
    return identifier.location();
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("Variable \"%s\"\n".formatted(identifier.name));
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Variable that = (Variable) o;
    return identifier.equals(that.identifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier);
  }
}

/**
 * Any kind of binary expression (often written with the infix notation in vadl).
 */
class BinaryExpr extends Expr {
  Expr left;
  Operation operation;
  Expr right;

  BinaryExpr(Expr left, Operation operation, Expr right) {
    this.left = left;
    this.operation = operation;
    this.right = right;
  }

  String operationAsString(Operation op) {
    return switch (op) {
      case ADD -> "+";
      case SUBTRACT -> "-";
      case MULTIPLY -> "*";
      case DIVIDE -> "/";
    };
  }

  @Override
  Location location() {
    return new Location(left.location(), right.location());
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("BinaryExpr (operation: %s)\n".formatted(operationAsString(operation)));
    left.dump(indent + 1, builder);
    right.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    // FIXME: Remove the parenthesis in the future and determine if they are needed
    builder.append("(");
    left.prettyPrint(indent, builder);
    builder.append(" %s ".formatted(operationAsString(operation)));
    right.prettyPrint(indent, builder);
    builder.append(")");
  }

  enum Operation {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BinaryExpr that = (BinaryExpr) o;
    return Objects.equals(left, that.left) && operation == that.operation
        && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(left);
    result = 31 * result + Objects.hashCode(operation);
    result = 31 * result + Objects.hashCode(right);
    return result;
  }
}

