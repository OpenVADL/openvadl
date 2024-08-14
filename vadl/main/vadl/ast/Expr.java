package vadl.ast;

import com.google.common.base.Preconditions;
import java.util.List;
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
  R visit(Identifier expr);

  R visit(BinaryExpr expr);

  R visit(GroupedExpr expr);

  R visit(IntegerLiteral expr);

  R visit(BinaryLiteral expr);

  R visit(BoolLiteral expr);

  R visit(StringLiteral expr);

  R visit(PlaceholderExpr expr);

  R visit(MacroInstanceExpr expr);

  R visit(RangeExpr expr);

  R visit(TypeLiteral expr);

  R visit(IdentifierPath expr);

  R visit(UnaryExpr expr);

  R visit(CallExpr expr);

  R visit(IfExpr expr);

  R visit(LetExpr expr);

  R visit(CastExpr expr);

  R visit(SymbolExpr expr);

  R visit(OperatorExpr expr);
}

final class Identifier extends Expr implements IsId, IdentifierOrPlaceholder {
  String name;
  SourceLocation loc;

  public Identifier(String name, SourceLocation location) {
    this.loc = location;
    this.name = name;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Id();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(name);
  }

  @Override
  public String pathToString() {
    return name;
  }

  @Override
  public String toString() {
    return "%s name: \"%s\"".formatted(this.getClass().getSimpleName(), this.name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Identifier that = (Identifier) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }
}

sealed interface OperatorOrPlaceholder permits OperatorExpr, PlaceholderExpr, MacroInstanceExpr {
}

/**
 * The operator class provides singleton constructors for immutable instances for each operator.
 */
@SuppressWarnings("checkstyle:methodname")
class Operator {
  final String symbol;
  final int precedence;

  private Operator(String symbol, int precedence) {
    this.symbol = symbol;
    this.precedence = precedence;
  }

  private static final int precLogicalOr = 0;
  private static final int precLogicalAnd = precLogicalOr + 1;
  private static final int precIn = precLogicalAnd + 1;
  private static final int precOr = precIn + 1;
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
  private static final Operator opIn = new Operator("in", precIn);
  private static final Operator opNotIn = new Operator("!in", precIn);

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

  static Operator In() {
    return opIn;
  }

  static Operator NotIn() {
    return opNotIn;
  }
}

enum UnaryOperator {
  NEGATIVE("-"), LOG_NOT("!"), COMPLEMENT("~");

  final String symbol;

  UnaryOperator(String symbol) {
    this.symbol = symbol;
  }
}

final class OperatorExpr extends Expr implements OperatorOrPlaceholder {

  Operator operator;
  SourceLocation location;

  OperatorExpr(Operator operator, SourceLocation location) {
    this.operator = operator;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.BinOp();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(operator.symbol);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " " + operator.symbol;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OperatorExpr that = (OperatorExpr) o;
    return Objects.equals(operator, that.operator);
  }

  @Override
  public int hashCode() {
    return operator.hashCode();
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }
}

/**
 * Any kind of binary expression (often written with the infix notation in vadl).
 */
class BinaryExpr extends Expr {
  Expr left;
  OperatorOrPlaceholder operator;
  Expr right;

  BinaryExpr(Expr left, OperatorOrPlaceholder operator, Expr right) {
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  static @Nullable BinaryExpr root = null;

  /**
   * This method reorders a left-sided source expression tree
   * into operator precedence order (as shown in the graph).
   * It mutates the "left" and "right" properties of the expression tree members.
   * <pre>
   *       *            -
   *      / \          / \
   *     *   4   =>   1   *
   *    / \              / \
   *   -   3            *   4
   *  / \              / \
   * 1   2            2   3
   * </pre>
   * Terminology and proof of this algorithm is presented in the article
   * <a href="https://dl.acm.org/doi/pdf/10.1145/357121.357127">by Lalonde and Des Rivieres</a>.
   *
   * @param expr A left-sided binary expression tree.
   * @return the root of the expression tree in operator precedence order
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
      if (par.operator().precedence > curr.operator().precedence) {
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

  Operator operator() {
    return ((OperatorExpr) operator).operator;
  }

  @Override
  SourceLocation location() {
    return left.location().join(right.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Ex();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    // FIXME: Remove the parenthesis in the future and determine if they are needed
    builder.append("(");
    left.prettyPrint(indent, builder);
    builder.append(" %s ".formatted(operator().symbol));
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
        operator().symbol);
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

class UnaryExpr extends Expr {
  UnaryOperator operator;
  Expr operand;

  UnaryExpr(UnaryOperator operation, Expr operand) {
    this.operator = operation;
    this.operand = operand;
  }

  @Override
  SourceLocation location() {
    return operand.location().join(operand.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.UnOp();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(operator.symbol);
    operand.prettyPrint(indent, builder);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s operator: %s".formatted(this.getClass().getSimpleName(), operator.symbol);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnaryExpr that = (UnaryExpr) o;
    return operator.equals(that.operator) && Objects.equals(operand, that.operand);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(operator);
    result = 31 * result + Objects.hashCode(operand);
    return result;
  }
}

class IntegerLiteral extends Expr {
  String token;
  long number;
  SourceLocation loc;

  private static long parse(String token) {
    return Long.parseLong(token.replace("'", ""));
  }

  public IntegerLiteral(String token, SourceLocation loc) {
    this.token = token;
    this.number = parse(token);
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Int();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(token);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s literal: %s (%d)".formatted(this.getClass().getSimpleName(), token, number);
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
    return number == that.number && token.equals(that.token);
  }

  @Override
  public int hashCode() {
    int result = Long.hashCode(number);
    result = 31 * result + Objects.hashCode(token);
    return result;
  }
}

class BinaryLiteral extends Expr {
  String token;
  long number;
  SourceLocation loc;

  private static long parse(String token) {
    token = token.replace("'", "");
    if (token.startsWith("0x")) {
      return Long.parseLong(token.substring(2), 16);
    } else if (token.startsWith("0b")) {
      return Long.parseLong(token.substring(2), 2);
    } else {
      throw new IllegalArgumentException("No conversion implemented for binary literal " + token);
    }
  }

  public BinaryLiteral(String token, SourceLocation loc) {
    this.token = token;
    this.number = parse(token);
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Bin();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(token);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s literal: %s (%d)".formatted(this.getClass().getSimpleName(), token, number);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BinaryLiteral that = (BinaryLiteral) o;
    return number == that.number && token.equals(that.token);
  }

  @Override
  public int hashCode() {
    int result = Long.hashCode(number);
    result = 31 * result + Objects.hashCode(token);
    return result;
  }
}

class BoolLiteral extends Expr {
  boolean value;
  SourceLocation loc;

  BoolLiteral(boolean value, SourceLocation loc) {
    this.value = value;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Bool();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(value);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s literal: %s".formatted(this.getClass().getSimpleName(), value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BoolLiteral that = (BoolLiteral) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return Boolean.hashCode(value);
  }
}

class StringLiteral extends Expr {
  String token;
  String value;
  SourceLocation loc;

  public StringLiteral(String token, SourceLocation loc) {
    this.token = token;
    this.value = StringLiteralParser.parseString(token.substring(1, token.length() - 1));
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Str();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(token);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s literal: \"%s\" (%s)".formatted(this.getClass().getSimpleName(), value, token);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StringLiteral that = (StringLiteral) o;
    return token.equals(that.token);
  }

  @Override
  public int hashCode() {
    return token.hashCode();
  }
}

sealed interface IdentifierOrPlaceholder permits Identifier, PlaceholderExpr, MacroInstanceExpr {
  void prettyPrint(int indent, StringBuilder builder);
}

/**
 * An internal temporary placeholder node inside model definitions.
 * This node should never leave the parser.
 */
final class PlaceholderExpr extends Expr implements IdentifierOrPlaceholder,
    OperatorOrPlaceholder, TypeLiteralOrPlaceholder, FieldEncodingsOrPlaceholder, IsId {
  IsCallExpr placeholder;
  SourceLocation loc;

  public PlaceholderExpr(IsCallExpr placeholder, SourceLocation loc) {
    this.placeholder = placeholder;
    this.loc = loc;
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Invalid();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append("$");
    placeholder.prettyPrint(indent, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PlaceholderExpr that = (PlaceholderExpr) o;
    return placeholder.equals(that.placeholder);
  }

  @Override
  public int hashCode() {
    return placeholder.hashCode();
  }

  @Override
  public String pathToString() {
    return "$" + placeholder.path().pathToString();
  }
}

/**
 * An internal temporary placeholder of macro instantiations.
 * This node should never leave the parser.
 */
final class MacroInstanceExpr extends Expr implements IdentifierOrPlaceholder,
    OperatorOrPlaceholder, TypeLiteralOrPlaceholder, FieldEncodingsOrPlaceholder, IsId {
  Macro macro;
  List<Node> arguments;
  SourceLocation loc;

  public MacroInstanceExpr(Macro macro, List<Node> arguments, SourceLocation loc) {
    this.macro = macro;
    this.arguments = arguments;
    this.loc = loc;
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Invalid();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("$");
    builder.append(macro.name().name);
    builder.append("(");
    var isFirst = true;
    for (var arg : arguments) {
      if (!isFirst) {
        builder.append(" ; ");
      }
      isFirst = false;
      arg.prettyPrint(0, builder);
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

    MacroInstanceExpr that = (MacroInstanceExpr) o;
    return macro.equals(that.macro)
        && arguments.equals(that.arguments);
  }

  @Override
  public int hashCode() {
    int result = macro.hashCode();
    result = 31 * result + arguments.hashCode();
    return result;
  }

  @Override
  public String pathToString() {
    var sb = new StringBuilder();
    prettyPrint(0, sb);
    return sb.toString();
  }
}

/**
 * A grouped expression.
 * Grouped expressions can either be single expressions wrapped in parantheses like {@code (1 + 2)},
 * or multiple expressions separated by a comma like {@code (a, 1 + 2, c())}.
 */
class GroupedExpr extends Expr {
  List<Expr> expressions;
  SourceLocation loc;

  public GroupedExpr(List<Expr> expressions, SourceLocation loc) {
    this.expressions = expressions;
    this.loc = loc;
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    // This node should never leave the parser and therefore never meet a visitor.
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Ex();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("(");
    var isFirst = true;
    for (var expr : expressions) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      expr.prettyPrint(0, builder);
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

    GroupedExpr that = (GroupedExpr) o;
    return expressions.equals(that.expressions);
  }

  @Override
  public int hashCode() {
    return expressions.hashCode();
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
  SyntaxType syntaxType() {
    return BasicSyntaxType.Invalid();
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

sealed interface TypeLiteralOrPlaceholder permits TypeLiteral, PlaceholderExpr, MacroInstanceExpr {
}

/**
 * TypeLiterals are needed as the types are not known during parsing.
 * For example {@code Bits<counter>} depends on the constant {@code counter} used here and so some
 * constant evaluation has to be performed for the concrete type to be known here.
 */
final class TypeLiteral extends Expr implements TypeLiteralOrPlaceholder {
  IsId baseType;

  /**
   * The sizes of the type literal. An expression of {@code <1,2><3,4>} is equivalent to
   * a sizeIndices of {@code List.of(List.of(1, 2), List.of(3, 4))}
   */
  List<List<Expr>> sizeIndices;

  SourceLocation loc;

  public TypeLiteral(IsId baseType, List<List<Expr>> sizeIndices, SourceLocation loc) {
    this.baseType = baseType;
    this.sizeIndices = sizeIndices;
    this.loc = loc;
  }

  public TypeLiteral(IsSymExpr symExpr) {
    this.baseType = symExpr.path();
    var size = symExpr.size();
    this.sizeIndices = size == null ? List.of() : List.of(List.of(size));
    this.loc = symExpr.location();
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Invalid();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(baseType.pathToString());
    for (var sizes : sizeIndices) {
      builder.append("<");
      var isFirst = true;
      for (var size : sizes) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        size.prettyPrint(0, builder);
      }
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
        && Objects.equals(sizeIndices, that.sizeIndices);
  }

  @Override
  public int hashCode() {
    int result = baseType.hashCode();
    result = 31 * result + Objects.hashCode(sizeIndices);
    return result;
  }
}

sealed interface IsCallExpr permits CallExpr, IsSymExpr {
  IsId path();

  @Nullable
  Expr size();

  List<List<Expr>> argsIndices();

  List<CallExpr.SubCall> subCalls();

  SourceLocation location();

  void prettyPrint(int indent, StringBuilder builder);
}

sealed interface IsSymExpr extends IsCallExpr permits SymbolExpr, IsId {
  @Override
  IsId path();

  @Override
  @Nullable
  Expr size();

  @Override
  default List<List<Expr>> argsIndices() {
    return List.of();
  }

  @Override
  default List<CallExpr.SubCall> subCalls() {
    return List.of();
  }
}

sealed interface IsId extends IsSymExpr
    permits IdentifierPath, Identifier, PlaceholderExpr, MacroInstanceExpr {
  @Override
  default IsId path() {
    return this;
  }

  @Override
  default @Nullable Expr size() {
    return null;
  }

  String pathToString();
}

final class IdentifierPath extends Expr implements IsId {
  /**
   * List of segments in this path; the first N-1 segments are (nested) namespaces,
   * the last segment is an identifier in the (nested) namespace.
   * Size has to be at least 1
   */
  List<IdentifierOrPlaceholder> segments;

  public IdentifierPath(List<IdentifierOrPlaceholder> segments) {
    Preconditions.checkArgument(!segments.isEmpty(),
        "IdentifierPath needs at least one Identifier");
    this.segments = segments;
  }

  @Override
  public SourceLocation location() {
    var first = (Node) segments.get(0);
    var last = (Node) segments.get(segments.size() - 1);
    return first.location().join(last.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Id();
  }

  @Override
  public String pathToString() {
    StringBuilder sb = new StringBuilder();
    prettyPrint(0, sb);
    return sb.toString();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    var isFirst = true;
    for (var segment : segments) {
      if (!isFirst) {
        builder.append("::");
      }
      isFirst = false;
      ((Node) segment).prettyPrint(indent, builder);
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

    IdentifierPath that = (IdentifierPath) o;
    return segments.equals(that.segments);
  }

  @Override
  public int hashCode() {
    return segments.hashCode();
  }
}

/**
 * A representation of terms of form {@code "MEM<9>"}.
 */
final class SymbolExpr extends Expr implements IsSymExpr {
  IsId path;
  Expr size;
  SourceLocation location;

  SymbolExpr(IsId path, Expr size, SourceLocation location) {
    this.path = path;
    this.size = size;
    this.location = location;
  }

  @Override
  public IsId path() {
    return path;
  }

  @Override
  public Expr size() {
    return size;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.SymEx();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    path.prettyPrint(indent, builder);
    builder.append("< ");
    size.prettyPrint(indent, builder);
    builder.append(" >");
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

    SymbolExpr that = (SymbolExpr) o;
    return path.equals(that.path) && Objects.equals(size, that.size);
  }

  @Override
  public int hashCode() {
    int result = path.hashCode();
    result = 31 * result + Objects.hashCode(size);
    return result;
  }
}

final class CallExpr extends Expr implements IsCallExpr {
  IsSymExpr target;
  /**
   * A list of function arguments or register/memory indices,
   * where multidimensional index access is represented as multiple list entries.
   */
  List<List<Expr>> argsIndices;
  /**
   * A list of method or sub-field access, e.g. the {@code .bar()} in {@code Namespace::Foo.bar()}.
   * Each sub-call can itself also have single- and multidimensional arguments.
   */
  List<SubCall> subCalls;
  SourceLocation location;

  public CallExpr(IsSymExpr target, List<List<Expr>> argsIndices,
                  List<SubCall> subCalls, SourceLocation location) {
    this.target = target;
    this.argsIndices = argsIndices;
    this.subCalls = subCalls;
    this.location = location;
  }

  @Override
  public IsId path() {
    return target.path();
  }

  @Override
  public @Nullable Expr size() {
    return target.size();
  }

  @Override
  public List<List<Expr>> argsIndices() {
    return argsIndices;
  }

  @Override
  public List<SubCall> subCalls() {
    return subCalls;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.CallEx();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    target.prettyPrint(indent, builder);
    printArgsIndices(argsIndices, builder);
    for (var subCall : subCalls) {
      builder.append(".");
      subCall.id.prettyPrint(0, builder);
      printArgsIndices(subCall.argsIndices, builder);
    }
  }

  private void printArgsIndices(List<List<Expr>> argsIndices, StringBuilder builder) {
    for (var args : argsIndices) {
      builder.append("(");
      boolean first = true;
      for (var arg : args) {
        if (!first) {
          builder.append(", ");
        }
        arg.prettyPrint(0, builder);
        first = false;
      }
      builder.append(")");
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

    CallExpr that = (CallExpr) o;
    return target.equals(that.target)
        && argsIndices.equals(that.argsIndices)
        && subCalls.equals(that.subCalls);
  }

  @Override
  public int hashCode() {
    int result = target.hashCode();
    result = 31 * result + Objects.hashCode(argsIndices);
    result = 31 * result + Objects.hashCode(subCalls);
    return result;
  }

  record SubCall(Identifier id, List<List<Expr>> argsIndices) {
  }
}

class IfExpr extends Expr {
  Expr condition;
  Expr thenExpr;
  Expr elseExpr;
  SourceLocation location;

  IfExpr(Expr condition, Expr thenExpr, Expr elseExpr, SourceLocation location) {
    this.condition = condition;
    this.thenExpr = thenExpr;
    this.elseExpr = elseExpr;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Ex();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("if ");
    condition.prettyPrint(indent, builder);
    builder.append(" then\n");
    thenExpr.prettyPrint(indent + 1, builder);
    builder.append("\n").append(prettyIndentString(indent)).append("else\n");
    elseExpr.prettyPrint(indent + 1, builder);
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

    IfExpr that = (IfExpr) o;
    return condition.equals(that.condition)
        && thenExpr.equals(that.thenExpr)
        && elseExpr.equals(that.elseExpr);
  }

  @Override
  public int hashCode() {
    int result = condition.hashCode();
    result = 31 * result + Objects.hashCode(thenExpr);
    result = 31 * result + Objects.hashCode(elseExpr);
    return result;
  }
}

class LetExpr extends Expr {
  List<Identifier> identifiers;
  Expr valueExpr;
  Expr body;
  SourceLocation location;

  LetExpr(List<Identifier> identifiers, Expr valueExpr, Expr body, SourceLocation location) {
    this.identifiers = identifiers;
    this.valueExpr = valueExpr;
    this.body = body;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Ex();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("let ");
    var isFirst = true;
    for (var identifier : identifiers) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      identifier.prettyPrint(indent, builder);
    }
    builder.append(" = ");
    valueExpr.prettyPrint(indent + 1, builder);
    builder.append(" in\n");
    body.prettyPrint(indent + 1, builder);
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

    LetExpr that = (LetExpr) o;
    return identifiers.equals(that.identifiers)
        && valueExpr.equals(that.valueExpr)
        && body.equals(that.body);
  }

  @Override
  public int hashCode() {
    int result = identifiers.hashCode();
    result = 31 * result + Objects.hashCode(valueExpr);
    result = 31 * result + Objects.hashCode(body);
    return result;
  }
}

class CastExpr extends Expr {
  Expr value;
  TypeLiteralOrPlaceholder type;

  public CastExpr(Expr value, TypeLiteralOrPlaceholder type) {
    this.value = value;
    this.type = type;
  }

  @Override
  SourceLocation location() {
    return value.location().join(((Expr) type).location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Ex();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    value.prettyPrint(indent, builder);
    builder.append(" as ");
    ((Expr) type).prettyPrint(indent, builder);
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

    CastExpr that = (CastExpr) o;
    return value.equals(that.value) && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + Objects.hashCode(type);
    return result;
  }
}
