package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

// TODO Extend Node
sealed interface Statement permits BlockStatement, LetStatement, IfStatement, AssignmentStatement {
  void prettyPrint(int indent, StringBuilder builder);

  default <T> T accept(StatementVisitor<T> visitor) {
    // TODO Use exhaustive switch with patterns in future Java versions
    if (this instanceof BlockStatement b) {
      return visitor.visit(b);
    } else if (this instanceof LetStatement l) {
      return visitor.visit(l);
    } else if (this instanceof IfStatement i) {
      return visitor.visit(i);
    } else if (this instanceof AssignmentStatement a) {
      return visitor.visit(a);
    } else {
      throw new IllegalStateException("Unhandled statement type " + getClass().getSimpleName());
    }
  }
}

interface StatementVisitor<T> {
  T visit(BlockStatement blockStatement);
  T visit(LetStatement letStatement);
  T visit(IfStatement ifStatement);
  T visit(AssignmentStatement assignmentStatement);
}

record BlockStatement(List<Statement> statements) implements Statement {
  BlockStatement() {
    this(new ArrayList<>());
  }

  BlockStatement add(Statement statement) {
    statements.add(statement);
    return this;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(2 * indent));
    builder.append("{\n");
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

record AssignmentStatement(Expr target, Expr valueExpression) implements Statement {
  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(2 * indent));
    target.prettyPrint(0, builder);
    builder.append(" := ");
    valueExpression.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }
}
