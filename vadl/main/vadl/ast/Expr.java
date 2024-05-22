package vadl.ast;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

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
    DIVIDE;

    Precedence precedence() {
      return switch (this) {
        case ADD, SUBTRACT -> Precedence.TERM;
        case MULTIPLY, DIVIDE -> Precedence.FACTOR;
      };
    }
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

  /**
   * Reorders binary expression based on the correct precedence
   *
   * @param expr to reorder
   * @return the new root of the reordered subtree
   */
  public static BinaryExpr reorder(BinaryExpr expr) {
    var precedence = expr.operation.precedence();

    // Reorder left
    if (expr.left instanceof BinaryExpr left &&
        precedence.greaterThan(left.operation.precedence())) {
      var temp = left.right;
      left.right = expr;
      expr.left = temp;

      // Since the reorder we maybe need to reorder the new top again:
      return reorder(left);
    }

    // Reorder left
    if (expr.right instanceof BinaryExpr right &&
        precedence.greaterThan(right.operation.precedence())) {
      var temp = right.left;
      right.left = expr;
      expr.right = temp;

      // Since the reorder we maybe need to reorder the new top again:
      return reorder(right);
    }

    expr.left = PseudoGroupExpr.ungroup(expr.left);
    expr.right = PseudoGroupExpr.ungroup(expr.right);
    return expr;
  }

  @Override
  SourceLocation location() {
    return left.location().join(right.location());
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
  public String toString() {
    return "%s operator: %s".formatted(this.getClass().getSimpleName(),
        operationAsString(operation));
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
  SourceLocation loc;

  public IntegerLiteral(long number, SourceLocation loc) {
    this.number = number;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
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
  public String toString() {
    return "%s number: %d".formatted(this.getClass().getSimpleName(), number);
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
 * A intermediate group expression during parsing.
 * This node should never leave the parser.
 */
class PseudoGroupExpr extends Expr {
  Expr expression;

  public PseudoGroupExpr(Expr expression) {
    this.expression = expression;
  }

  static Expr ungroup(Expr expr) {
    while (expr instanceof PseudoGroupExpr) {
      expr = ((PseudoGroupExpr) expr).expression;
    }
    return expr;
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    // This node should never leave the parser and therefore never meet a visitor.
    throw new RuntimeException("Intentionally not implemented");
  }

  @Override
  SourceLocation location() {
    return expression.location();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    // This node should never leave the parser and therefore never meet a visitor.
    throw new RuntimeException("Intentionally not implemented");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PseudoGroupExpr that = (PseudoGroupExpr) o;
    return expression.equals(that.expression);
  }

  @Override
  public int hashCode() {
    return expression.hashCode();
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
  SourceLocation location() {
    return from.location().join(to.location());
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    from.prettyPrint(indent, builder);
    builder.append("..");
    to.prettyPrint(indent, builder);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
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

  SourceLocation loc;

  public TypeLiteral(Identifier baseType, @Nullable Expr sizeExpression, SourceLocation loc) {
    this.baseType = baseType;
    this.sizeExpression = sizeExpression;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
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
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
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
  SourceLocation location() {
    return identifier.location();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
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
