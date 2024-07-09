package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

// TODO Extend Node
interface Statement {
  void prettyPrint(int indent, StringBuilder builder);
}

record BlockStatement(List<Statement> statements) implements Statement {
  BlockStatement() {
    this(new ArrayList<>());
  }

  void add(Statement statement) {
    statements.add(statement);
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(2 * indent));
    builder.append("{");
    statements.forEach(statement -> statement.prettyPrint(indent + 1, builder));
    builder.append(" ".repeat(2 * indent));
    builder.append("}");
  }
}

record LetStatement(Identifier identifier, Expr valueExpression, Statement body)
    implements Statement {
  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(2 * indent));
    builder.append("let ");
    builder.append(identifier.name);
    builder.append(" = ");
    valueExpression.prettyPrint(indent + 1, builder);
    builder.append(" in\n");
    body.prettyPrint(indent + 1, builder);
  }
}

record IfStatement(Expr condition, Statement thenStmt, @Nullable Statement elseStmt)
    implements Statement {
  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(2 * indent));
    builder.append("if ");
    condition.prettyPrint(indent + 1, builder);
    builder.append(" then\n");
    thenStmt.prettyPrint(indent + 1, builder);
    builder.append("\n");
    if (elseStmt != null) {
      builder.append(" ".repeat(2 * indent));
      builder.append("else\n");
      elseStmt.prettyPrint(indent + 1, builder);
      builder.append("\n");
    }
  }
}

record AssignmentStatement(Identifier identifier, Expr valueExpression) implements Statement {
  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(2 * indent));
    builder.append(identifier.name);
    builder.append(" := ");
    valueExpression.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }
}
