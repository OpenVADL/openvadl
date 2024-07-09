package vadl.ast;

import java.util.ArrayList;
import java.util.List;

interface Statement {
  void prettyPrint(int indent, StringBuilder builder);
}

record Block(List<Statement> statements) implements Statement {
  Block() {
    this(new ArrayList<>());
  }

  void add(Statement statement) {
    statements.add(statement);
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    statements.forEach(statement -> statement.prettyPrint(indent, builder));
  }
}

record LetStatement(Identifier identifier, Expr valueExpression, Block block) implements Statement {
  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(" ".repeat(2 * indent));
    builder.append("let ");
    builder.append(identifier.name);
    builder.append(" = ");
    valueExpression.prettyPrint(indent + 1, builder);
    builder.append(" in\n");
    block.prettyPrint(indent + 1, builder);
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
