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
 * The operator class provides singleton constructors for immutable instances for each operator.
 */
class Operator {
  final String symbol;
  final int precedence;

  private Operator(String symbol, int precedence) {
    this.symbol = symbol;
    this.precedence = precedence;
  }

  private static final int precLogicalOr = 0;
  private static final int precLogicalAnd = precLogicalOr + 1;
  private static final int precOr = precLogicalAnd + 1;
  private static final int precXor = precOr + 1;
  private static final int precAnd = precXor + 1;
  private static final int precEquality = precAnd + 1;
  private static final int precComparison = precEquality + 1;
  private static final int precShift = precComparison + 1;
  private static final int precTerm = precShift + 1;
  private static final int precFactor = precTerm + 1;

  private static final Operator opLogicalOr = new Operator("||", precLogicalOr);
  private static final Operator opLogicalAnd = new Operator("&&", precLogicalAnd);
  private static final Operator opOr = new Operator("|", precOr);
  private static final Operator opXor = new Operator("^", precXor);
  private static final Operator opAnd = new Operator("&", precAnd);
  private static final Operator opEqual = new Operator("=", precEquality);
  private static final Operator opNotEqual = new Operator("!=", precEquality);
  private static final Operator opGreaterEqual = new Operator(">=", precComparison);
  private static final Operator opGreater = new Operator(">", precComparison);
  private static final Operator opLessEqual = new Operator("<=", precComparison);
  private static final Operator opLess = new Operator("<", precComparison);
  private static final Operator opRotateRight = new Operator("<>>", precShift);
  private static final Operator opRotateLeft = new Operator("<<>", precShift);
  private static final Operator opShiftRight = new Operator(">>", precShift);
  private static final Operator opShiftLeft = new Operator("<<", precShift);
  private static final Operator opAdd = new Operator("+", precTerm);
  private static final Operator opSubtract = new Operator("-", precTerm);
  private static final Operator opMultiply = new Operator("*", precFactor);
  private static final Operator opDivide = new Operator("/", precFactor);
  private static final Operator opModulo = new Operator("%", precFactor);

  static Operator LogicalOr() {
    return opLogicalOr;
  }

  static Operator LogicalAnd() {
    return opLogicalAnd;
  }

  static Operator Or() {
    return opOr;
  }

  static Operator Xor() {
    return opXor;
  }

  static Operator And() {
    return opAnd;
  }

  static Operator Equal() {
    return opEqual;
  }

  static Operator NotEqual() {
    return opNotEqual;
  }

  static Operator GreaterEqual() {
    return opGreaterEqual;
  }

  static Operator Greater() {
    return opGreater;
  }

  static Operator LessEqual() {
    return opLessEqual;
  }

  static Operator Less() {
    return opLess;
  }


  static Operator RotateRight() {
    return opRotateRight;
  }

  static Operator RotateLeft() {
    return opRotateLeft;
  }

  static Operator ShiftRight() {
    return opShiftRight;
  }

  static Operator ShiftLeft() {
    return opShiftLeft;
  }

  static Operator Add() {
    return opAdd;
  }

  static Operator Subtract() {
    return opSubtract;
  }

  static Operator Multiply() {
    return opMultiply;
  }

  static Operator Divide() {
    return opDivide;
  }

  static Operator Modulo() {
    return opModulo;
  }
}

/**
 * Any kind of binary expression (often written with the infix notation in vadl).
 */
class BinaryExpr extends Expr {
  Expr left;
  Operator operator;
  Expr right;

  BinaryExpr(Expr left, Operator operation, Expr right) {
    this.left = left;
    this.operator = operation;
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
      if (par.operator.precedence > curr.operator.precedence) {
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
    builder.append(" %s ".formatted(operator.symbol));
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
        operator.symbol);
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
    return Objects.equals(left, that.left) && operator.equals(that.operator)
        && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(left);
    result = 31 * result + Objects.hashCode(operator);
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
