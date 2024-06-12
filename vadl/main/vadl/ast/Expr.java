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

  R visit(GroupExpr expr);

  R visit(IntegerLiteral expr);

  R visit(InternalErrorExpr expr);

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
    LOGICAL_OR,
    LOGICAL_AND,
    OR,
    XOR,
    AND,
    EQUAL,
    NOTEQUAL,
    GREATER_EQUAL,
    GREATER,
    LESS_EQUAL,
    LESS,
    ROTATE_RIGHT,
    ROTATE_LEFT,
    SHIFT_RIGHT,
    SHIFT_LEFT,
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    ;

    Precedence precedence() {
      return switch (this) {
        case LOGICAL_OR -> Precedence.LOGICAL_OR;
        case LOGICAL_AND -> Precedence.LOGICAL_AND;
        case OR -> Precedence.OR;
        case XOR -> Precedence.XOR;
        case AND -> Precedence.AND;
        case EQUAL, NOTEQUAL -> Precedence.EQUALITY;
        case LESS, LESS_EQUAL, GREATER, GREATER_EQUAL -> Precedence.COMPARISON;
        case SHIFT_LEFT, SHIFT_RIGHT, ROTATE_LEFT, ROTATE_RIGHT -> Precedence.SHIFT;
        case ADD, SUBTRACT -> Precedence.TERM;
        case MULTIPLY, DIVIDE, MODULO -> Precedence.FACTOR;
      };
    }

    String toSymbol() {
      return switch (this) {
        case LOGICAL_OR -> "||";
        case LOGICAL_AND -> "&&";
        case OR -> "|";
        case XOR -> "^";
        case AND -> "&";
        case NOTEQUAL -> "!=";
        case EQUAL -> "==";
        case GREATER_EQUAL -> ">=";
        case GREATER -> ">";
        case LESS_EQUAL -> "<=";
        case LESS -> "<";
        case ROTATE_RIGHT -> "<>>";
        case ROTATE_LEFT -> "<<>";
        case SHIFT_RIGHT -> ">>";
        case SHIFT_LEFT -> "<<";
        case ADD -> "+";
        case SUBTRACT -> "-";
        case MULTIPLY -> "*";
        case DIVIDE -> "/";
        case MODULO -> "%";
      };
    }
  }

  BinaryExpr(Expr left, Operation operation, Expr right) {
    this.left = left;
    this.operation = operation;
    this.right = right;
  }

  static @Nullable BinaryExpr root = null;

  /**
   * Reorders binary expression based on the correct precedence
   *
   * @param expr to reorder
   * @return the new root of the reordered subtree
   */
  static BinaryExpr reorder(BinaryExpr expr) {
    root = expr;
    transformRecRightToLeft(null, expr);
    if (root == null) {
      throw new RuntimeException("Should never happen");
    }
    return root;
  }

  static BinaryExpr transformRecRightToLeft(@Nullable BinaryExpr parpar, BinaryExpr par) {
    while (par.left instanceof BinaryExpr curr) {
      if (par.operation.precedence().greaterThan(curr.operation.precedence())) {
        par.left = curr.right;
        curr.right = par;
        if ((par = parpar) != null) {
          par.left = curr;
          return par;
        }
        root = curr;
      }
      par = transformRecRightToLeft(par, curr);
    }
    return par;
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
    builder.append(" %s ".formatted(operation.toSymbol()));
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
        operation.toSymbol());
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
 * An internal temporary expression node.
 */
class InternalErrorExpr extends Expr {
  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return SourceLocation.INVALID_SOURCE_LOCATION;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("/* INTERNAL ERROR */");
  }
}

/**
 * A group expression.
 */
class GroupExpr extends Expr {
  Expr inner;

  public GroupExpr(Expr expression) {
    this.inner = expression;
  }

  static Expr ungroup(Expr expr) {
    while (expr instanceof GroupExpr) {
      expr = ((GroupExpr) expr).inner;
    }
    return expr;
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    // This node should never leave the parser and therefore never meet a visitor.
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return inner.location();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    // This node should never leave the parser and therefore never meet a visitor.
    builder.append("(");
    inner.prettyPrint(indent, builder);
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

    GroupExpr that = (GroupExpr) o;
    return inner.equals(that.inner);
  }

  @Override
  public int hashCode() {
    return inner.hashCode();
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
