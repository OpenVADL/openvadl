// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.BitsType;
import vadl.types.BoolType;
import vadl.types.BuiltInTable;
import vadl.types.ConcreteRelationType;
import vadl.types.SIntType;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.utils.SourceLocation;
import vadl.utils.WithLocation;
import vadl.viam.Constant;

/**
 * The Expression node of the AST.
 */
@SuppressWarnings("EnumOrdinal")
public abstract class Expr extends Node implements TypedNode {
  @Nullable
  Type type = null;

  @Override
  public Type type() {
    return Objects.requireNonNull(type);
  }

  Precedence precedence() {
    return Precedence.NoPrecedence;
  }

  abstract void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec);

  /**
   * Wraps the expression in parentheses depending on this precedence and the
   * parent's one.
   *
   * <p>If weak is true, we don't care about grouping expressions with the same precedence,
   * which is fine for `as` and unary operations.
   * However, when weak is false (i.e. binaryOps),
   * we must group expressions with the same precedences
   * for safety reasons: {@code e.g. a >> b << c != a >> (b << c)}
   *
   * @param parentPrec precedence of parent
   * @param sb         string builder to add parentheses
   * @param weak       if true parentheses are not added when parent has same precedence
   * @param builder    lambda that builds the inner expression printing
   */
  void wrapInGroup(Precedence parentPrec, StringBuilder sb, boolean weak, Runnable builder) {
    if (parentPrec.ordinal() > precedence().ordinal()
        || (!weak && parentPrec.ordinal() == precedence().ordinal())) {
      sb.append("(");
      builder.run();
      sb.append(")");
    } else {
      builder.run();
    }
  }

  @Override
  public final void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
  }

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

  R visit(CallIndexExpr expr);

  R visit(IfExpr expr);

  R visit(LetExpr expr);

  R visit(CastExpr expr);

  R visit(SymbolExpr expr);

  R visit(MacroMatchExpr expr);

  R visit(MatchExpr expr);

  R visit(AsIdExpr expr);

  R visit(AsStrExpr expr);

  R visit(ExistsInExpr expr);

  R visit(ExistsInThenExpr expr);

  R visit(ForallThenExpr expr);

  R visit(ForallExpr expr);

  R visit(SequenceCallExpr expr);

  R visit(ExpandedSequenceCallExpr expr);

  R visit(ExpandedAliasDefSequenceCallExpr expr);
}

final class Identifier extends Expr implements IsId, IdentifierOrPlaceholder {
  String name;
  SourceLocation loc;

  /**
   * The node this identifier refers to.
   */
  @Nullable
  Node target;

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
    return BasicSyntaxType.ID;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(name);
  }

  @Override
  public String pathToString() {
    return name;
  }

  @Nullable
  @Override
  public Node target() {
    return target;
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

sealed interface IsBinOp permits BinOp, PlaceholderNode, MacroInstanceNode, MacroMatchNode {
  void prettyPrint(int indent, StringBuilder builder);
}

sealed interface IsUnOp permits UnOp, PlaceholderNode, MacroInstanceNode, MacroMatchNode {
  void prettyPrint(int indent, StringBuilder builder);
}

enum Precedence {
  NoPrecedence,
  LogicalOr,
  LogicalAnd,
  In,
  Or,
  Xor,
  And,
  Equality,
  Comparison,
  Shift,
  Term,
  Factor,
  CastOp,
  UnaryOp,
}

/**
 * The operator class provides singleton constructors for immutable instances for each operator.
 */
@SuppressWarnings("checkstyle:methodname")
class Operator {
  final String symbol;
  final Precedence precedence;

  private Operator(String symbol, Precedence precedence) {
    this.symbol = symbol;
    this.precedence = precedence;
  }

  @Override
  public String toString() {
    return symbol;
  }

  public static final Operator LogicalOr = new Operator("||", Precedence.LogicalOr);
  public static final Operator LogicalAnd = new Operator("&&", Precedence.LogicalAnd);
  public static final Operator Or = new Operator("|", Precedence.Or);
  public static final Operator Xor = new Operator("^", Precedence.Xor);
  public static final Operator And = new Operator("&", Precedence.And);
  public static final Operator Equal = new Operator("=", Precedence.Equality);
  public static final Operator NotEqual = new Operator("!=", Precedence.Equality);
  public static final Operator GreaterEqual = new Operator(">=", Precedence.Comparison);
  public static final Operator Greater = new Operator(">", Precedence.Comparison);
  public static final Operator LessEqual = new Operator("<=", Precedence.Comparison);
  public static final Operator Less = new Operator("<", Precedence.Comparison);
  public static final Operator RotateRight = new Operator("<>>", Precedence.Shift);
  public static final Operator RotateLeft = new Operator("<<>", Precedence.Shift);
  public static final Operator ShiftRight = new Operator(">>", Precedence.Shift);
  public static final Operator ShiftLeft = new Operator("<<", Precedence.Shift);
  public static final Operator Add = new Operator("+", Precedence.Term);
  public static final Operator Subtract = new Operator("-", Precedence.Term);
  public static final Operator SaturatedAdd = new Operator("+|", Precedence.Term);
  public static final Operator SaturatedSubtract = new Operator("-|", Precedence.Term);
  public static final Operator Multiply = new Operator("*", Precedence.Factor);
  public static final Operator Divide = new Operator("/", Precedence.Factor);
  public static final Operator Modulo = new Operator("%", Precedence.Factor);
  public static final Operator LongMultiply = new Operator("*#", Precedence.Factor);
  public static final Operator In = new Operator("in", Precedence.In);
  public static final Operator NotIn = new Operator("!in", Precedence.In);
  public static final Operator ElementOf = new Operator("∈", Precedence.In);
  public static final Operator NotElementOf = new Operator("∉", Precedence.In);

  public static final List<Operator> allOperators = List.of(
      LogicalOr,
      LogicalAnd,
      Or,
      Xor,
      And,
      Equal,
      NotEqual,
      GreaterEqual,
      Greater,
      LessEqual,
      Less,
      RotateRight,
      RotateLeft,
      ShiftRight,
      ShiftLeft,
      Add,
      Subtract,
      SaturatedAdd,
      SaturatedSubtract,
      Multiply,
      Divide,
      Modulo,
      LongMultiply,
      In,
      NotIn,
      ElementOf,
      NotElementOf
  );
  public static final List<Operator> logicalComparisions = List.of(LogicalOr, LogicalAnd);
  public static final List<Operator> arithmeticOperators =
      List.of(Or, Xor, And, RotateRight, RotateLeft, ShiftLeft, ShiftRight, Add, Subtract, Multiply,
          Divide, Modulo, LongMultiply);
  public static final List<Operator> artihmeticComparisons =
      List.of(Equal, NotEqual, GreaterEqual, Greater, LessEqual, Less
      );

  @Nullable
  public static Operator fromString(String operator) {
    return allOperators.stream().filter(op -> op.symbol.equals(operator)).findFirst().orElse(null);
  }
}

enum UnaryOperator {
  NEGATIVE("-"), LOG_NOT("!"), COMPLEMENT("~");

  final String symbol;

  UnaryOperator(String symbol) {
    this.symbol = symbol;
  }

  public static UnaryOperator fromSymbol(String symbol) {
    for (UnaryOperator op : UnaryOperator.values()) {
      if (op.symbol.equals(symbol)) {
        return op;
      }
    }
    throw new IllegalArgumentException("No operator with symbol: " + symbol);
  }
}

/**
 * Any kind of binary expression (often written with the infix notation in vadl).
 */
class BinaryExpr extends Expr {
  @Child
  Expr left;
  IsBinOp operator;
  @Child
  Expr right;
  boolean hasBeenReordered = false;

  BinaryExpr(Expr left, IsBinOp operator, Expr right) {
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

  @SuppressWarnings("EnumOrdinal")
  static BinaryExpr transformRecRightToLeft(@Nullable BinaryExpr parpar, BinaryExpr par) {
    par.hasBeenReordered = true;
    while (par.left instanceof BinaryExpr curr) {
      if (par.operator().precedence.ordinal() > curr.operator().precedence.ordinal()) {
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
    return ((BinOp) operator).operator;
  }

  @Override
  Precedence precedence() {
    return operator().precedence;
  }

  @Override
  public SourceLocation location() {
    return left.location().join(right.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }


  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    wrapInGroup(parentPrec, builder, false, () -> {
      left.prettyPrintExpr(indent, builder, precedence());
      builder.append(" ");
      operator.prettyPrint(0, builder);
      builder.append(" ");
      right.prettyPrintExpr(indent, builder, precedence());
    });
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s operator: %s".formatted(this.getClass().getSimpleName(), operator().symbol);
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
  IsUnOp operator;
  @Child
  Expr operand;

  /**
   * The builtin that will be called.
   * Set by the typechecker.
   */
  @Nullable
  BuiltInTable.BuiltIn computedTarget;

  UnaryExpr(IsUnOp operator, Expr operand) {
    this.operator = operator;
    this.operand = operand;
  }

  UnOp unOp() {
    return (UnOp) operator;
  }

  @Override
  Precedence precedence() {
    return Precedence.UnaryOp;
  }

  @Override
  public SourceLocation location() {
    return operand.location().join(operand.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    operator.prettyPrint(indent, builder);
    wrapInGroup(parentPrec, builder, true, () -> {
      operand.prettyPrintExpr(indent, builder, precedence());
    });
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s operator: %s".formatted(this.getClass().getSimpleName(), operator);
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
  BigInteger number;
  SourceLocation loc;

  private static BigInteger parse(String token) {
    return new BigInteger(token.replace("'", ""));
  }

  public IntegerLiteral(String token, SourceLocation loc) {
    this.token = token;
    this.number = parse(token);
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INT;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
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
    return number.equals(that.number) && token.equals(that.token);
  }

  @Override
  public int hashCode() {
    int result = number.hashCode();
    result = 31 * result + Objects.hashCode(token);
    return result;
  }
}

/**
 * A hex or binary integer literal.
 * Example: `0xaf` for hex or `0b01` for binary.
 */
class BinaryLiteral extends Expr {
  String token;
  BigInteger number;
  int bitWidth;
  SourceLocation loc;

  public BinaryLiteral(String token, SourceLocation loc) {
    this.token = token;
    this.loc = loc;

    var simplifiedToken = token.replace("'", "");
    if (token.startsWith("0x")) {
      this.number = new BigInteger(simplifiedToken.substring(2), 16);
      this.bitWidth = (simplifiedToken.length() - 2) * 4;
    } else if (simplifiedToken.startsWith("0b")) {
      this.number = new BigInteger(simplifiedToken.substring(2), 2);
      this.bitWidth = (simplifiedToken.length() - 2);
    } else {
      throw new IllegalArgumentException("No conversion implemented for binary literal " + token);
    }
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.BIN;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
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
    return number.equals(that.number) && token.equals(that.token);
  }

  @Override
  public int hashCode() {
    int result = number.hashCode();
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
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.BOOL;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
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

  // FIXME: Cleanup StringLiteral constructors, each one does interpret their arguments a bit
  // differently, which is quite confusing to use.

  public StringLiteral(String token, SourceLocation loc) {
    this.token = token;
    if (token.length() > 1) {
      this.value = StringLiteralParser.parseString(token.substring(1, token.length() - 1));
    } else {
      this.value = token;
    }
    this.loc = loc;
  }

  public StringLiteral(Identifier fromId, SourceLocation loc) {
    // TODO More robust string escaping - only used for prettifying expanded AsStr code
    this.token = '"' + fromId.name.replaceAll("\"", "\\\"") + '"';
    this.value = fromId.name;
    this.loc = loc;
  }

  public StringLiteral(String token) {
    this.token = '"' + token + '"';
    this.value = token;
    this.loc = SourceLocation.INVALID_SOURCE_LOCATION;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.STR;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
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

/**
 * Represents an {@link Identifier} or one of the placeholder sub types.
 * A placeholder is some macro expr that cannot be directly expanded to an {@link Identifier}, as
 * itself is part of a macro definition.
 *
 * <p>For instance in the following {@code $constId} is such a placeholder:
 * <pre>{@code
 * model Test (constId: Id) : Defs = {
 *   constant $constId = 4
 * }
 * }</pre></p>
 */
sealed interface IdentifierOrPlaceholder extends IsId
    permits Identifier, MacroInstanceExpr, MacroMatchExpr, PlaceholderExpr, AsIdExpr {
}

/**
 * An internal temporary placeholder node inside model definitions.
 * This node should never leave the parser.
 */
final class PlaceholderExpr extends Expr implements IdentifierOrPlaceholder, IsId {
  List<String> segments;
  SyntaxType syntaxType;
  SourceLocation loc;

  public PlaceholderExpr(List<String> segments, SyntaxType syntaxType, SourceLocation loc) {
    this.segments = segments;
    this.syntaxType = syntaxType;
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
    return syntaxType;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("$");
    builder.append(String.join(".", segments));
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
    return segments.equals(that.segments);
  }

  @Override
  public int hashCode() {
    return segments.hashCode();
  }

  @Override
  public String pathToString() {
    var sb = new StringBuilder();
    prettyPrint(0, sb);
    return sb.toString();
  }

  @Nullable
  @Override
  public Node target() {
    return null;
  }
}

/**
 * An internal temporary placeholder of macro instantiations.
 * This node should never leave the parser.
 */
final class MacroInstanceExpr extends Expr
    implements IsMacroInstance, IdentifierOrPlaceholder, IsId {
  MacroOrPlaceholder macro;
  List<Node> arguments;
  SourceLocation loc;

  public MacroInstanceExpr(MacroOrPlaceholder macro, List<Node> arguments, SourceLocation loc) {
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
    return macro.returnType();
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(prettyIndentString(indent));
    builder.append("$");
    if (macro instanceof Macro m) {
      builder.append(m.name().name);
    } else if (macro instanceof MacroPlaceholder mp) {
      builder.append(String.join(".", mp.segments()));
    }
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

  @Nullable
  @Override
  public Node target() {
    return null;
  }

  @Override
  public MacroOrPlaceholder macroOrPlaceholder() {
    return macro;
  }
}

/**
 * An internal temporary placeholder of a macro-level "match" construct.
 * This node should never leave the parser.
 */
final class MacroMatchExpr extends Expr implements IsMacroMatch, IdentifierOrPlaceholder, IsId {
  MacroMatch macroMatch;

  MacroMatchExpr(MacroMatch macroMatch) {
    this.macroMatch = macroMatch;
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return macroMatch.sourceLocation();
  }

  @Override
  SyntaxType syntaxType() {
    return macroMatch.resultType();
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    macroMatch.prettyPrint(indent, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MacroMatchExpr that = (MacroMatchExpr) o;
    return macroMatch.equals(that.macroMatch);
  }

  @Override
  public int hashCode() {
    return macroMatch.hashCode();
  }

  @Override
  public String pathToString() {
    return "/* Match - can't be rendered! */";
  }

  @Nullable
  @Override
  public Node target() {
    return null;
  }
}

/**
 * An internal temporary node representing the AsId lexical macro.
 * Produces identifiers from strings or is used to concatenate multiple identifiers/strings into
 * one.
 *
 * <p><pre>{@code
 *  constant AsId("one") = 1
 *  constant AsId("th", "ree") = 3
 *  constant AsId(max, count) = 42
 *  constant AsId(open, "vadl") = 2024
 *  }</pre>
 *
 * <p>This node should never leave the parser.
 */
final class AsIdExpr extends Expr implements IdentifierOrPlaceholder, IsId {
  GroupedExpr expr;
  SourceLocation loc;

  AsIdExpr(GroupedExpr expr, SourceLocation loc) {
    this.expr = expr;
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
    return BasicSyntaxType.ID;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("AsId ");
    expr.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AsIdExpr that = (AsIdExpr) o;
    return expr.equals(that.expr);
  }

  @Override
  public int hashCode() {
    return expr.hashCode();
  }

  @Override
  public String pathToString() {
    var sb = new StringBuilder();
    prettyPrint(0, sb);
    return sb.toString();
  }

  @Nullable
  @Override
  public Node target() {
    return null;
  }
}

/**
 * An internal temporary node representing the AsStr lexical macro.
 * Produces string literals from identifiers or is used to concatenate multiple
 * identifiers/strings into one.
 *
 * <p><pre>{@code
 *  function one -> String = AsStr(one)
 *  function three -> String = AsStr(th, ree)
 *  function maxcount -> String = AsStr("max", "count")
 *  function openvadl -> String = AsStr("open", vadl)
 *  }</pre>
 *
 * <p>This node should never leave the parser.
 */
final class AsStrExpr extends Expr {
  GroupedExpr expr;
  SourceLocation loc;

  AsStrExpr(GroupedExpr expr, SourceLocation loc) {
    this.expr = expr;
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
    return BasicSyntaxType.STR;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("AsStr ");
    expr.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AsStrExpr that = (AsStrExpr) o;
    return expr.equals(that.expr);
  }

  @Override
  public int hashCode() {
    return expr.hashCode();
  }
}


/**
 * A grouped expression.
 * Grouped expressions can either be single expressions wrapped in parantheses like {@code (1 + 2)},
 * or multiple expressions separated by a comma like {@code (a, 1 + 2, c())}.
 *
 * <p>A group expression with a single expression is just arithmetic grouping, but a group
 * expression with multiple is a bits or string concatenation.
 */
// FIXME: This should probably be two nodes as the semantics are so different.
class GroupedExpr extends Expr {
  @Child
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
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("(");
    var isFirst = true;
    for (var expr : expressions) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      expr.prettyPrintExpr(0, builder, Precedence.NoPrecedence);
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
  @Child
  Expr from;
  @Child
  Expr to;

  public RangeExpr(Expr from, Expr to) {
    this.from = from;
    this.to = to;
  }

  @Override
  public SourceLocation location() {
    return from.location().join(to.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    from.prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
    builder.append("..");
    to.prettyPrint(indent, builder);
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
final class TypeLiteral extends Expr {
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

  // A constructor used internally when the type is already known
  public TypeLiteral(Type type, SourceLocation loc) {
    this.type = type;
    this.loc = loc;

    if (type.getClass() == BoolType.class) {
      this.baseType = new Identifier("Bool", SourceLocation.INVALID_SOURCE_LOCATION);
      this.sizeIndices = List.of(List.of());
    } else if (type.getClass() == BitsType.class) {
      var bitsType = (BitsType) type;
      this.baseType = new Identifier("Bits", SourceLocation.INVALID_SOURCE_LOCATION);
      this.sizeIndices =
          List.of(List.of(new IntegerLiteral(Integer.toString(bitsType.bitWidth()), loc)));
    } else if (type.getClass() == UIntType.class) {
      var uintType = (UIntType) type;
      this.baseType = new Identifier("UInt", SourceLocation.INVALID_SOURCE_LOCATION);
      this.sizeIndices =
          List.of(List.of(new IntegerLiteral(Integer.toString(uintType.bitWidth()), loc)));
    } else if (type.getClass() == SIntType.class) {
      var sintType = (SIntType) type;
      this.baseType = new Identifier("SInt", SourceLocation.INVALID_SOURCE_LOCATION);
      this.sizeIndices =
          List.of(List.of(new IntegerLiteral(Integer.toString(sintType.bitWidth()), loc)));
    } else if (type.getClass() == FormatType.class) {
      var formatType = (FormatType) type;
      this.baseType = new Identifier(formatType.format.identifier().name,
          SourceLocation.INVALID_SOURCE_LOCATION);
      this.sizeIndices =
          List.of(List.of());
    } else {
      throw new IllegalStateException("Unsupported type " + type.getClass().getSimpleName());
    }
  }

  @Override
  public List<Node> children() {
    // This is too complicated for the @Child annotation
    var childNodes = new ArrayList<Node>();
    childNodes.add((Node) baseType);
    childNodes.addAll(sizeIndices.stream()
        .flatMap(Collection::stream)
        .toList()
    );
    return childNodes.stream().filter(Objects::nonNull).toList();
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(baseType.pathToString());
    for (var sizes : sizeIndices) {
      builder.append("<");
      var isFirst = true;
      for (var size : sizes) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        size.prettyPrintExpr(0, builder, Precedence.NoPrecedence);
      }
      builder.append(">");
    }
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

sealed interface IsCallExpr extends WithLocation permits CallIndexExpr, IsSymExpr {
  IsId path();

  @Nullable
  Expr size();

  void prettyPrint(int indent, StringBuilder builder);
}

sealed interface IsSymExpr extends IsCallExpr permits SymbolExpr, IsId {
  @Override
  IsId path();

  @Override
  @Nullable
  Expr size();
}

sealed interface IsId extends IsSymExpr
    permits AsIdExpr, Identifier, IdentifierOrPlaceholder, IdentifierPath, MacroInstanceExpr,
    MacroMatchExpr, PlaceholderExpr {
  @Override
  default IsId path() {
    return this;
  }

  @Override
  default @Nullable Expr size() {
    return null;
  }

  String pathToString();

  /**
   * The target this id refers to. It is resolved during symbol resolving and
   * is only valid for {@link Identifier} and {@link IdentifierPath}, which
   * are the only two {@link IsId} subtypes that survive the {@link MacroExpander}.
   */
  @Nullable
  Node target();
}

final class IdentifierPath extends Expr implements IsId {
  /**
   * List of segments in this path; the first N-1 segments are (nested) namespaces,
   * the last segment is an identifier in the (nested) namespace.
   * Size has to be at least 1
   */
  List<IdentifierOrPlaceholder> segments;

  /**
   * The node this identifier refers to.
   */
  @Nullable
  Node target;

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
    return BasicSyntaxType.ID;
  }

  @Override
  public String pathToString() {
    StringBuilder sb = new StringBuilder();
    prettyPrint(0, sb);
    return sb.toString();
  }

  @Nullable
  @Override
  public Node target() {
    return target;
  }

  //  @Override
  public List<String> pathToSegments() {
    // FIXME: There must be a better solution
    return List.of(pathToString().split("::"));
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
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
  @Child
  IsId path;
  @Child
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
    return BasicSyntaxType.SYM_EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    path.prettyPrint(indent, builder);
    builder.append("< ");
    size.prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
    builder.append(" >");
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

/**
 * A call expression or indexing or many similar expressions.
 *
 * <p>The expression represents zero or one calls followed by zero or many indexing/slicing.
 *
 * <p>The following are also call expressions:
 * - Memory access: Mem<4>(addr)
 * - Slicing: target(4..8)
 * - Access of fields: PC.next ... a subcall
 *
 * <p>One call expr can have multiple "calls":
 * - Accessing a Register File and slicing: X(0)(3..7)
 */
final class CallIndexExpr extends Expr implements IsCallExpr {
  IsSymExpr target;

  /**
   * A list of function arguments or register/memory indices.
   *
   * <p>Because, a callExpr can actually represent multiple calls this is a list of lists.
   */
  List<Arguments> argsIndices;

  /**
   * A list of method or sub-field access, e.g. the {@code .bar()} in {@code Namespace::Foo.bar()}.
   * Each sub-call can itself also have single- and multidimensional arguments.
   */
  List<SubCall> subCalls;
  SourceLocation location;

  /**
   * If the call points to a builtin this field is set instead of computedTarget.
   */
  @Nullable
  BuiltInTable.BuiltIn computedBuiltIn;

  @Nullable
  Type typeBeforeSlice;

  public CallIndexExpr(IsSymExpr target, List<Arguments> argsIndices, List<SubCall> subCalls,
                       SourceLocation location) {
    this.target = target;
    this.argsIndices = argsIndices;
    this.subCalls = subCalls;
    this.location = location;
  }

  public Node computedTarget() {
    return Objects.requireNonNull(target.path().target());
  }

  public Type typeBeforeSlice() {
    return Objects.requireNonNull(typeBeforeSlice);
  }

  /**
   * Returns the {@link Arguments} for a call to some definition.
   * As arguments must (e.g. function call arguments or register indices) must not be mixed
   * with slices in the same group, the returned list provides ONLY arguments (no slices).
   *
   * <p>If the target has a {@link ConcreteRelationType}, there will only be a single argument
   * returned.
   * If the target is a register and has a tensor type, multiple arguments might be returned,
   * as access to multidimensional registers may be written in separate groups.</p>
   *
   * <p><b>NOTE:</b>This method may only be called if the type checker has already
   * resolved either the {@link #computedBuiltIn} or {@link #computedTarget()}, otherwise
   * calling this function will result in a crash.
   * (so it is generally safe after the type check)</p>
   */
  // FIXME: Implement access to multidimensional registers
  public List<Arguments> args() {
    if (computedBuiltIn != null) {
      // if we reference a built-in, we must check if the built-in takes arguments
      if (computedBuiltIn.argTypeClasses().isEmpty()
          || argsIndices.isEmpty()) {
        return List.of();
      }
      return List.of(argsIndices.getFirst());
    }

    if (computedTarget() instanceof LetExpr) {
      // let expressions don't have any arguments.
      // they are typed nodes, but arguments referring to let exprs are not
      // using the let expr's type, but just the value defined in it.
      // so the type would be null when calling .type()
      return List.of();
    }

    if (computedTarget() instanceof TypedNode typedNode) {
      var type = typedNode.type();
      if (type instanceof ConcreteRelationType relType) {
        if (relType.argTypes().isEmpty()) {
          // relation types that don't expect an argument don't have any argument groups
          return List.of();
        }
        return argsIndices.isEmpty() ? List.of() : List.of(argsIndices.getFirst());
      }
      // in the case of a register:
      // arguments are all argument groups that don't start with a range expression and
      // don't exceed the number of tensor indices.
    }
    return List.of();
  }

  /**
   * Returns a list of all argument groups that represent slices on the result
   * of the call.
   */
  public List<Arguments> slices() {
    return argsIndices.subList(args().size(), argsIndices.size());
  }

  @Override
  List<Node> children() {
    // This is too complicated for the @Child annotation
    List<Node> childNodes = new ArrayList<>();
    childNodes.add((Node) target);
    childNodes.addAll(argsIndices.stream().flatMap(a -> a.values.stream()).toList());
    childNodes.addAll(subCalls.stream()
        .flatMap(subCall -> subCall.argsIndices.stream()
            .flatMap(a -> a.values.stream()))
        .toList());
    return childNodes.stream().filter(Objects::nonNull).toList();
  }

  void replaceArgsFor(int index, List<Expr> newArgs) {
    var args = this.argsIndices.get(index);
    if (args.values.size() != newArgs.size()) {
      throw new IllegalStateException();
    }
    args.values.clear();
    args.values.addAll(newArgs);
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
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.CALL_EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    target.prettyPrint(indent, builder);
    printArgsIndices(argsIndices, builder);
    for (var subCall : subCalls) {
      builder.append(".");
      subCall.id.prettyPrint(0, builder);
      printArgsIndices(subCall.argsIndices, builder);
    }
  }

  private void printArgsIndices(List<Arguments> argsIndices, StringBuilder builder) {
    for (var args : argsIndices) {
      builder.append("(");
      boolean first = true;
      for (var arg : args.values) {
        if (!first) {
          builder.append(", ");
        }
        arg.prettyPrintExpr(0, builder, Precedence.NoPrecedence);
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CallIndexExpr that = (CallIndexExpr) o;
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

  static final class Arguments implements TypedNode {
    List<Expr> values;
    SourceLocation location;

    // FIXME: I think this type does not make sense here.
    @Nullable
    Type type;

    @Nullable
    Constant.BitSlice computedBitSlice;

    Arguments(List<Expr> values, SourceLocation location) {
      this.values = values;
      this.location = location;
    }

    @Override
    public Type type() {
      return Objects.requireNonNull(type);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Arguments) obj;
      return Objects.equals(this.values, that.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(values);
    }


  }

  static final class SubCall {
    Identifier id;
    List<Arguments> argsIndices;

    /**
     * If the subcall is a format fieldaccess the type of that access is stored here.
     * This does ignore further manipulation by the argsIndicies.
     */
    @Nullable
    Type formatFieldType;

    /**
     * If the subcall is a format fieldaccess the range of that access is stored here.
     * This does ignore further manipulation by the argsIndicies.
     */
    @Nullable
    Constant.BitSlice computedBitSlice;

    /**
     * If the subcall is status access, this field tells which index in the status type the
     * field is.
     */
    @Nullable
    public Integer computedStatusIndex;

    SubCall(Identifier id, List<Arguments> argsIndices) {
      this.id = id;
      this.argsIndices = argsIndices;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (SubCall) obj;
      return Objects.equals(this.id, that.id)
          && Objects.equals(this.argsIndices, that.argsIndices);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, argsIndices);
    }

  }
}

class IfExpr extends Expr {
  @Child
  Expr condition;
  @Child
  Expr thenExpr;
  @Child
  Expr elseExpr;
  SourceLocation location;

  IfExpr(Expr condition, Expr thenExpr, Expr elseExpr, SourceLocation location) {
    this.condition = condition;
    this.thenExpr = thenExpr;
    this.elseExpr = elseExpr;
    this.location = location;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(prettyIndentString(indent));
    builder.append("if ");
    condition.prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
    builder.append(" then\n");
    if (!isBlockLayout(thenExpr)) {
      builder.append(prettyIndentString(indent + 1));
    }
    thenExpr.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
    builder.append("\n").append(prettyIndentString(indent)).append("else\n");
    if (!isBlockLayout(elseExpr)) {
      builder.append(prettyIndentString(indent + 1));
    }
    elseExpr.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
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
  @Child
  Expr valueExpr;
  @Child
  Expr body;
  SourceLocation location;

  LetExpr(List<Identifier> identifiers, Expr valueExpr, Expr body, SourceLocation location) {
    this.identifiers = identifiers;
    this.valueExpr = valueExpr;
    this.body = body;
    this.location = location;
  }

  /**
   * Returns the index of one of the variables the statement defines.
   *
   * @return the type of the name provided.
   */
  int getIndexOf(String name) {
    return identifiers.stream().map(i -> i.name).toList().indexOf(name);
  }

  /**
   * Returns the type of one of the variables the statement defines.
   *
   * @return the type of the name provided.
   */
  Type getTypeOf(String name) {
    var valType = valueExpr.type;
    if (identifiers.size() == 1) {
      return Objects.requireNonNull(valType);
    }

    if (!(valType instanceof TupleType valTuple)) {
      throw new IllegalStateException("Expected TupleType but got " + valType);
    }

    return Objects.requireNonNull(valTuple.get(getIndexOf(name)));
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(prettyIndentString(indent));
    builder.append("(let ");
    var isFirst = true;
    for (var identifier : identifiers) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      identifier.prettyPrint(indent, builder);
    }
    builder.append(" = ");
    valueExpr.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
    builder.append(" in\n");
    if (!isBlockLayout(body)) {
      builder.append(prettyIndentString(indent + 1));
    }
    body.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
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
  @Child
  Expr value;
  @Child
  TypeLiteral typeLiteral;
  SourceLocation location;

  public CastExpr(Expr value, TypeLiteral typeLiteral) {
    this.value = value;
    this.typeLiteral = typeLiteral;
    this.location = value.location().join(typeLiteral.location());
  }

  public CastExpr(Expr value, Type type) {
    this.value = value;
    this.type = type;
    this.location = value.location();
    this.typeLiteral = new TypeLiteral(type, value.location());
  }

  @Override
  Precedence precedence() {
    return Precedence.CastOp;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    wrapInGroup(parentPrec, builder, true, () -> {
      value.prettyPrintExpr(indent, builder, Precedence.CastOp);
      builder.append(" as ");
      typeLiteral.prettyPrint(indent, builder);
    });
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

    CastExpr that = (CastExpr) o;
    return value.equals(that.value) && typeLiteral.equals(that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + Objects.hashCode(typeLiteral);
    return result;
  }
}

class MatchExpr extends Expr {
  Expr candidate;
  List<Case> cases;
  Expr defaultResult;
  SourceLocation loc;

  MatchExpr(Expr candidate, List<Case> cases, Expr defaultResult, SourceLocation loc) {
    this.candidate = candidate;
    this.cases = cases;
    this.defaultResult = defaultResult;
    this.loc = loc;
  }

  @Override
  List<Node> children() {
    // This is too complicated for the @Child annotation
    var childNodes = new ArrayList<Node>();
    childNodes.add(candidate);
    cases.forEach(c -> {
      childNodes.addAll(c.patterns);
      childNodes.add(c.result);
    });
    childNodes.add(defaultResult);
    return childNodes.stream().filter(Objects::nonNull).toList();
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(prettyIndentString(indent)).append("match ");
    candidate.prettyPrint(0, builder);
    builder.append(" with\n");
    builder.append(prettyIndentString(indent + 1)).append("{ ");
    var isFirst = true;
    for (var matchCase : cases) {
      if (!isFirst) {
        builder.append(prettyIndentString(indent + 1)).append(", ");
      }
      isFirst = false;
      if (matchCase.patterns.size() == 1) {
        matchCase.patterns.get(0).prettyPrint(0, builder);
      } else {
        builder.append("{");
        var isFirstPattern = true;
        for (var pattern : matchCase.patterns) {
          if (!isFirstPattern) {
            builder.append(", ");
          }
          isFirstPattern = false;
          pattern.prettyPrint(0, builder);
        }
        builder.append("}");
      }
      builder.append(" => ");
      matchCase.result.prettyPrint(0, builder);
      builder.append("\n");
    }
    builder.append(prettyIndentString(indent + 1)).append(", _ => ");
    defaultResult.prettyPrint(0, builder);
    builder.append("\n").append(prettyIndentString(indent + 1)).append("}\n");
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

    MatchExpr that = (MatchExpr) o;
    return Objects.equals(candidate, that.candidate)
        && Objects.equals(cases, that.cases)
        && Objects.equals(defaultResult, that.defaultResult);
  }

  @Override
  public int hashCode() {
    int result = cases.hashCode();
    result = 31 * result + cases.hashCode();
    result = 31 * result + defaultResult.hashCode();
    return result;
  }

  static class Case {
    List<Expr> patterns;
    Expr result;

    public Case(List<Expr> patterns, Expr result) {
      this.patterns = patterns;
      this.result = result;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Case other = (Case) o;
      return patterns.equals(other.patterns) && result.equals(other.result);
    }

    @Override
    public int hashCode() {
      int result1 = patterns.hashCode();
      result1 = 31 * result1 + result.hashCode();
      return result1;
    }
  }
}

class ExistsInExpr extends Expr {
  List<IsId> operations;
  SourceLocation loc;

  ExistsInExpr(List<IsId> operations, SourceLocation loc) {
    this.operations = operations;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("exists in {");
    var isFirst = true;
    for (IsId operation : operations) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      operation.prettyPrint(0, builder);
    }
    builder.append("}");
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
    ExistsInExpr that = (ExistsInExpr) o;
    return Objects.equals(operations, that.operations);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(operations);
  }
}

class ExistsInThenExpr extends Expr {
  List<Condition> conditions;
  @Child
  Expr thenExpr;
  SourceLocation loc;

  ExistsInThenExpr(List<Condition> conditions, Expr thenExpr, SourceLocation loc) {
    this.conditions = conditions;
    this.thenExpr = thenExpr;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("exists ");
    var isFirst = true;
    for (Condition condition : conditions) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      condition.id.prettyPrint(0, builder);
      builder.append(" in {");
      var isFirstOp = true;
      for (IsId operation : condition.operations) {
        if (!isFirstOp) {
          builder.append(", ");
        }
        isFirstOp = false;
        operation.prettyPrint(0, builder);
      }
      builder.append("}");
    }
    if (isBlockLayout(thenExpr)) {
      builder.append(" then\n");
      thenExpr.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" then ");
      thenExpr.prettyPrint(0, builder);
      builder.append("\n");
    }
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
    ExistsInThenExpr that = (ExistsInThenExpr) o;
    return Objects.equals(conditions, that.conditions)
        && Objects.equals(thenExpr, that.thenExpr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, thenExpr);
  }

  record Condition(IsId id, List<IsId> operations) {
  }
}

class ForallThenExpr extends Expr {
  @Child
  List<ForallThenExpr.Index> indices;
  @Child
  Expr thenExpr;
  SourceLocation loc;

  ForallThenExpr(List<ForallThenExpr.Index> indices, Expr thenExpr, SourceLocation loc) {
    this.indices = indices;
    this.thenExpr = thenExpr;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("forall ");
    var isFirst = true;
    for (ForallThenExpr.Index index : indices) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      index.prettyPrint(indent, builder);

    }
    if (isBlockLayout(thenExpr)) {
      builder.append(" then\n");
      thenExpr.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" then ");
      thenExpr.prettyPrint(0, builder);
      builder.append("\n");
    }
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
    ForallThenExpr that = (ForallThenExpr) o;
    return Objects.equals(indices, that.indices)
        && Objects.equals(thenExpr, that.thenExpr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indices, thenExpr);
  }

  static final class Index extends Node implements IdentifiableNode {
    IsId id;
    List<IsId> operations;

    public Index(IsId id, List<IsId> operations) {
      this.id = id;
      this.operations = operations;
    }

    @Override
    public Identifier identifier() {
      return (Identifier) id;
    }

    @Override
    public SourceLocation location() {
      var loc = id.location();
      if (!operations.isEmpty()) {
        loc = loc.join(operations.get(operations.size() - 1).location());
      }
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      id.prettyPrint(0, builder);
      builder.append(" in {");
      var isFirstOp = true;
      for (IsId operation : operations) {
        if (!isFirstOp) {
          builder.append(", ");
        }
        isFirstOp = false;
        operation.prettyPrint(0, builder);
      }
      builder.append("}");
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Index index = (Index) o;
      return id.equals(index.id) && operations.equals(index.operations);
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + operations.hashCode();
      return result;
    }
  }
}

class ForallExpr extends Expr {
  List<ForallExpr.Index> indices;
  Operation operation;
  @Nullable
  Operator foldOperator;
  @Child
  Expr body;
  SourceLocation loc;

  ForallExpr(List<ForallExpr.Index> indices, Operation operation, @Nullable Operator foldOperator,
             Expr body, SourceLocation loc) {
    this.indices = indices;
    this.operation = operation;
    this.foldOperator = foldOperator;
    this.body = body;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("forall ");
    var isFirst = true;
    for (ForallExpr.Index index : indices) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      index.prettyPrint(indent, builder);
    }
    builder.append(" ").append(operation.keyword);
    if (foldOperator != null) {
      builder.append(" ").append(foldOperator.symbol).append(" with");
    }
    if (isBlockLayout(body)) {
      builder.append("\n");
      body.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" ");
      body.prettyPrint(0, builder);
      builder.append("\n");
    }
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
    ForallExpr that = (ForallExpr) o;
    return Objects.equals(indices, that.indices) && operation == that.operation
        && Objects.equals(foldOperator, that.foldOperator) && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indices, operation, foldOperator, body);
  }

  @Override
  public String toString() {
    return "%s keyword: %s, type: %s".formatted(getClass().getSimpleName(), operation.keyword,
        type);
  }

  static final class Index extends Node implements IdentifiableNode {
    IsId id;
    Expr domain;

    public Index(IsId id, Expr domain) {
      this.id = id;
      this.domain = domain;
    }

    @Override
    public Identifier identifier() {
      return (Identifier) id;
    }

    @Override
    public SourceLocation location() {
      return id.location().join(domain.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      id.prettyPrint(0, builder);
      builder.append(" in ");
      domain.prettyPrint(0, builder);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Index index = (Index) o;
      return id.equals(index.id) && domain.equals(index.domain);
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + domain.hashCode();
      return result;
    }
  }

  enum Operation {
    APPEND("append"), TENSOR("tensor"), FOLD("fold");

    private final String keyword;

    Operation(String keyword) {
      this.keyword = keyword;
    }
  }
}

class SequenceCallExpr extends Expr {

  @Child
  IsCallExpr target;
  @Nullable
  @Child
  Expr range;
  SourceLocation loc;

  SequenceCallExpr(IsCallExpr target, @Nullable Expr range, SourceLocation loc) {
    this.target = target;
    this.range = range;
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
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    target.prettyPrint(0, builder);
    if (range != null) {
      builder.append("{");
      range.prettyPrint(0, builder);
      builder.append("}");
    }
  }
}

/**
 * It is allowed to define shortcuts in {@link SequenceCallExpr}. These shortcuts will be
 * expanded into {@link ExpandedSequenceCallExpr} and {@link ExpandedAliasDefSequenceCallExpr}.
 * So {@code a{0..10}} will become {@code a0, a1, ...}. Each entry is then a
 * {@link ExpandedAliasDefSequenceCallExpr}. However, single entries need to be also represented.
 * If it is a simple entry like {@code a0} then this will be also to
 * {@link ExpandedAliasDefSequenceCallExpr} mapped. {@code X(1)} is a {@link CallIndexExpr} and will
 * be mapped to {@link ExpandedSequenceCallExpr}.
 */
sealed class ExpandedSequenceCallExpr extends Expr permits ExpandedAliasDefSequenceCallExpr {
  @Child
  Expr target;
  SourceLocation loc;

  ExpandedSequenceCallExpr(Expr target, SourceLocation loc) {
    this.target = target;
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
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    target.prettyPrint(0, builder);
  }
}

final class ExpandedAliasDefSequenceCallExpr extends ExpandedSequenceCallExpr {
  ExpandedAliasDefSequenceCallExpr(Identifier target,
                                   SourceLocation loc) {
    super(target, loc);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  List<Node> children() {
    // Remove this method when #293 is fixed.
    return List.of(target);
  }
}