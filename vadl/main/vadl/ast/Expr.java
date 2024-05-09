package vadl.ast;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The Expression node of the AST.
 */
public abstract class Expr extends Node {
  abstract <R> R accept(ExprVisitor<R> visitor);
}

interface ExprVisitor<R> {
  R visit(BinaryExpr expr);

  R visit(IntegerLiteral expr);

  R visit(RangeExpr expr);

  R visit(TypeLiteral expr);

  R visit(Variable expr);
}

/**
 * Any kind of binary expression (often written with the infix notation in vadl).
 */
class BinaryExpr extends Expr {
  Expr left;
  Operation operation;
  Expr right;

  enum Operation {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
  }

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
    builder.append(dumpIndentString(indent));
    builder.append("%s (operation: %s)\n".formatted(this.getClass().getSimpleName(),
        operationAsString(operation)));
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

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
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
    builder.append(dumpIndentString(indent));
    builder.append("%s (value: %d)\n".formatted(this.getClass().getSimpleName(), number));
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(number);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
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

    IntegerLiteral that = (IntegerLiteral) o;
    return number == that.number;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(number);
  }
}

class RangeExpr extends Expr {
  Expr from;
  Expr to;

  public RangeExpr(Expr from, Expr to) {
    this.from = from;
    this.to = to;
  }

  @Override
  Location location() {
    return new Location(from.location(), to.location());
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    from.dump(indent + 1, builder);
    to.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    from.prettyPrint(indent, builder);
    builder.append("..");
    to.prettyPrint(indent, builder);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RangeExpr rangeExpr = (RangeExpr) o;
    return from.equals(rangeExpr.from) && to.equals(rangeExpr.to);
  }

  @Override
  public int hashCode() {
    int result = from.hashCode();
    result = 31 * result + to.hashCode();
    return result;
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
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
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
  <R> R accept(ExprVisitor<R> visitor) {
    return null;
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
    builder.append(dumpIndentString(indent));
    builder.append(this.getClass().getSimpleName());
    builder.append("\n");
    identifier.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return null;
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
