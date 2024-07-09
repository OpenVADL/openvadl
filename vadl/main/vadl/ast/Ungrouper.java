package vadl.ast;

/**
 * Ungrouper, removes all group expressions recursively.
 * Groups are needed in the AST during parsing until all binary expressions are reordered but then
 * can be removed.
 */
class Ungrouper implements ExprVisitor<Expr> {

  public Expr ungroup(Expr expr) {
    return expr.accept(this);
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    expr.left = expr.left.accept(this);
    expr.right = expr.right.accept(this);
    return expr;
  }

  @Override
  public Expr visit(GroupExpr expr) {
    return expr.inner.accept(this);
  }

  @Override
  public Expr visit(IntegerLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(PlaceHolderExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(RangeExpr expr) {
    expr.to = expr.to.accept(this);
    expr.from = expr.from.accept(this);
    return expr;
  }

  @Override
  public Expr visit(TypeLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(Variable expr) {
    return expr;
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    expr.operand = expr.operand.accept(this);
    return expr;
  }
}
