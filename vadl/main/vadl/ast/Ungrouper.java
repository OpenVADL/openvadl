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
  public Expr visit(StringLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(PlaceholderExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(MacroInstanceExpr expr) {
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
  public Expr visit(IdentifierChain expr) {
    return expr;
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    expr.operand = expr.operand.accept(this);
    return expr;
  }

  @Override
  public Expr visit(CallExpr expr) {
    return new CallExpr(expr.identifier, expr.argument.accept(this));
  }

  @Override
  public Expr visit(IfExpr expr) {
    return new IfExpr(
        expr.condition.accept(this),
        expr.thenExpr.accept(this),
        expr.elseExpr.accept(this),
        expr.location
    );
  }

  @Override
  public Expr visit(LetExpr expr) {
    return new LetExpr(
        expr.identifier,
        expr.valueExpr.accept(this),
        expr.body.accept(this),
        expr.location
    );
  }
}
