package vadl.ast;

import java.util.ArrayList;

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
  public Expr visit(Identifier expr) {
    return expr;
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    expr.left = expr.left.accept(this);
    if (expr.right != null) {
      // Should never happen in a syntactically correct program.
      // In an expression like "3 > =", the parser will throw an error only after "ungroup"  is run
      expr.right = expr.right.accept(this);
    }
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
  public Expr visit(IdentifierPath expr) {
    return expr;
  }

  @Override
  public Expr visit(UnaryExpr expr) {
    expr.operand = expr.operand.accept(this);
    return expr;
  }

  @Override
  public Expr visit(CallExpr expr) {
    expr.target = (IsSymExpr) ((Expr) expr.target).accept(this);
    var argsIndices = expr.argsIndices;
    expr.argsIndices = new ArrayList<>(argsIndices.size());
    for (var entry : argsIndices) {
      var args = new ArrayList<Expr>(entry.size());
      for (var arg : entry) {
        args.add(arg.accept(this));
      }
      expr.argsIndices.add(args);
    }
    var subCalls = expr.subCalls;
    expr.subCalls = new ArrayList<>(subCalls.size());
    for (var subCall : subCalls) {
      argsIndices = new ArrayList<>(subCall.argsIndices().size());
      for (var entry : subCall.argsIndices()) {
        var args = new ArrayList<Expr>(entry.size());
        for (var arg : entry) {
          args.add(arg.accept(this));
        }
        argsIndices.add(args);
      }
      expr.subCalls.add(new CallExpr.SubCall(subCall.id(), argsIndices));
    }
    return expr;
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
        expr.identifiers,
        expr.valueExpr.accept(this),
        expr.body.accept(this),
        expr.location
    );
  }

  @Override
  public Expr visit(CastExpr expr) {
    expr.value = expr.value.accept(this);
    return expr;
  }

  @Override
  public Expr visit(SymbolExpr expr) {
    expr.size = expr.size.accept(this);
    return expr;
  }

  @Override
  public Expr visit(OperatorExpr expr) {
    return expr;
  }
}
