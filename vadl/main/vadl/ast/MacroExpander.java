package vadl.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.error.VadlError;
import vadl.error.VadlException;

/**
 * Expands and copies a macro template.
 */
class MacroExpander implements ExprVisitor<Expr> {
  Map<String, Node> args = new HashMap<>();
  List<VadlError> errors = new ArrayList<>();

  public Expr expandExpr(Expr expr, Map<String, Node> args) {
    this.args = args;
    var result = expr.accept(this);
    if (!errors.isEmpty()) {
      throw new VadlException(errors);
    }
    return result;
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    // FIXME: Only if parent is not a binary operator cause otherwise it is O(n^2)
    var result = new BinaryExpr(expr.left.accept(this), expr.operator, expr.right.accept(this));
    return BinaryExpr.reorder(result);
  }

  @Override
  public Expr visit(GroupExpr expr) {
    return new GroupExpr(expr.accept(this));
  }

  @Override
  public Expr visit(IntegerLiteral expr) {
    return new IntegerLiteral(expr.token, expr.loc);
  }

  @Override
  public Expr visit(StringLiteral expr) {
    return new StringLiteral(expr.value, expr.loc);
  }

  @Override
  public Expr visit(PlaceHolderExpr expr) {
    // FIXME: This could also be another macro
    var arg = (Expr) args.get(expr.identifier.name);
    if (arg == null) {
      throw new IllegalStateException("The parser should already have checked that.");
    }

    return arg.accept(this);
  }

  @Override
  public Expr visit(RangeExpr expr) {
    return new RangeExpr(expr.from.accept(this), expr.to.accept(this));
  }

  @Override
  public Expr visit(TypeLiteral expr) {
    var sizeExpression = expr.sizeExpression == null ? null : expr.sizeExpression.accept(this);
    return new TypeLiteral(expr.baseType, sizeExpression, expr.loc);
  }

  @Override
  public Expr visit(VariableAccess expr) {
    return new VariableAccess(expr.identifier, expr.next);
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    return new UnaryExpr(expr.operator, expr.operand.accept(this));
  }
}
