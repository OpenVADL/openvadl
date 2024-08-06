package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

abstract sealed class Statement extends Node
    permits BlockStatement, LetStatement, IfStatement, AssignmentStatement {
  <T> T accept(StatementVisitor<T> visitor) {
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

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Stat();
  }
}

interface StatementVisitor<T> {
  T visit(BlockStatement blockStatement);

  T visit(LetStatement letStatement);

  T visit(IfStatement ifStatement);

  T visit(AssignmentStatement assignmentStatement);
}

final class BlockStatement extends Statement {
  List<Statement> statements;
  SourceLocation location;

  BlockStatement(List<Statement> statements, SourceLocation location) {
    this.statements = statements;
    this.location = location;
  }

  BlockStatement(SourceLocation location) {
    this(new ArrayList<>(), location);
  }

  BlockStatement add(Statement statement) {
    statements.add(statement);
    return this;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("{\n");
    statements.forEach(statement -> statement.prettyPrint(indent + 1, builder));
    builder.append(prettyIndentString(indent));
    builder.append("}");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (BlockStatement) obj;
    return Objects.equals(this.statements, that.statements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statements);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}

final class LetStatement extends Statement {
  List<Identifier> identifiers;
  Expr valueExpression;
  Statement body;
  SourceLocation location;

  LetStatement(List<Identifier> identifiers, Expr valueExpression, Statement body,
               SourceLocation location) {
    this.identifiers = identifiers;
    this.valueExpression = valueExpression;
    this.body = body;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
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
    valueExpression.prettyPrint(indent + 1, builder);
    builder.append(" in\n");
    body.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (LetStatement) obj;
    return Objects.equals(this.identifiers, that.identifiers)
        && Objects.equals(this.valueExpression, that.valueExpression)
        && Objects.equals(this.body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifiers, valueExpression, body);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

final class IfStatement extends Statement {
  Expr condition;
  Statement thenStmt;
  @Nullable
  Statement elseStmt;
  SourceLocation location;

  IfStatement(Expr condition, Statement thenStmt, @Nullable Statement elseStmt,
              SourceLocation location) {
    this.condition = condition;
    this.thenStmt = thenStmt;
    this.elseStmt = elseStmt;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("if ");
    condition.prettyPrint(indent + 1, builder);
    builder.append(" then\n");
    thenStmt.prettyPrint(indent + 1, builder);
    builder.append("\n");
    if (elseStmt != null) {
      builder.append(prettyIndentString(indent));
      builder.append("else\n");
      elseStmt.prettyPrint(indent + 1, builder);
      builder.append("\n");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (IfStatement) obj;
    return Objects.equals(this.condition, that.condition)
        && Objects.equals(this.thenStmt, that.thenStmt)
        && Objects.equals(this.elseStmt, that.elseStmt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, thenStmt, elseStmt);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

final class AssignmentStatement extends Statement {
  Expr target;
  Expr valueExpression;

  AssignmentStatement(Expr target, Expr valueExpression) {
    this.target = target;
    this.valueExpression = valueExpression;
  }

  @Override
  SourceLocation location() {
    return target.location().join(valueExpression.location());
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    target.prettyPrint(0, builder);
    builder.append(" := ");
    valueExpression.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (AssignmentStatement) obj;
    return Objects.equals(this.target, that.target)
        && Objects.equals(this.valueExpression, that.valueExpression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target, valueExpression);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}

class StatementList extends Node {

  List<Statement> items;
  SourceLocation location;

  StatementList(List<Statement> items, SourceLocation location) {
    this.items = items;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.Stats();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    items.forEach(item -> item.prettyPrint(indent, builder));
  }
}
