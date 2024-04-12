package vadl.ast;

/**
 * The Statement nodes inside the AST.
 */
public abstract class Stmt extends Node {
}

class ExpressionStmt extends Stmt {
  final Expr expression;

  ExpressionStmt(Expr expression) {
    this.expression = expression;
  }

  @Override
  Location location() {
    return expression.location();
  }

  @Override
  void dump(int indent, StringBuilder builder) {
    builder.append(indentString(indent));
    builder.append("ExpressionStmt\n");

    expression.dump(indent + 1, builder);
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(indent * 4));
    expression.prettyPrint(indent, builder);
    builder.append("\n");
  }
}
