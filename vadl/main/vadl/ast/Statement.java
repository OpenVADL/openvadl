package vadl.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

abstract sealed class Statement extends Node
    permits AssignmentStatement, BlockStatement, CallStatement, ForallStatement, IfStatement,
    InstructionCallStatement, LetStatement, LockStatement, MacroInstanceStatement,
    MacroMatchStatement, MatchStatement, PlaceholderStatement, RaiseStatement, StatementList {
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
    } else if (this instanceof RaiseStatement r) {
      return visitor.visit(r);
    } else if (this instanceof CallStatement c) {
      return visitor.visit(c);
    } else if (this instanceof InstructionCallStatement ic) {
      return visitor.visit(ic);
    } else if (this instanceof PlaceholderStatement p) {
      return visitor.visit(p);
    } else if (this instanceof MacroInstanceStatement m) {
      return visitor.visit(m);
    } else if (this instanceof MacroMatchStatement m) {
      return visitor.visit(m);
    } else if (this instanceof MatchStatement m) {
      return visitor.visit(m);
    } else if (this instanceof StatementList s) {
      return visitor.visit(s);
    } else if (this instanceof LockStatement l) {
      return visitor.visit(l);
    } else if (this instanceof ForallStatement f) {
      return visitor.visit(f);
    } else {
      throw new IllegalStateException("Unhandled statement type " + getClass().getSimpleName());
    }
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.STAT;
  }
}

interface StatementVisitor<T> {
  T visit(BlockStatement blockStatement);

  T visit(LetStatement letStatement);

  T visit(IfStatement ifStatement);

  T visit(AssignmentStatement assignmentStatement);

  T visit(RaiseStatement raiseStatement);

  T visit(CallStatement callStatement);

  T visit(PlaceholderStatement placeholderStatement);

  T visit(MacroInstanceStatement macroInstanceStatement);

  T visit(MacroMatchStatement macroMatchStatement);

  T visit(MatchStatement matchStatement);

  T visit(StatementList statementList);

  T visit(InstructionCallStatement instructionCallStatement);

  T visit(LockStatement lockStatement);

  T visit(ForallStatement forallStatement);
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
    builder.append("}\n");
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
    if (isBlockLayout(valueExpression)) {
      builder.append(" :=\n");
    } else {
      builder.append(" := ");
    }
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

final class StatementList extends Statement {

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
    return BasicSyntaxType.STATS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    items.forEach(item -> item.prettyPrint(indent, builder));
  }
}

final class RaiseStatement extends Statement {

  Statement statement;
  SourceLocation location;

  RaiseStatement(Statement statement, SourceLocation location) {
    this.statement = statement;
    this.location = location;
  }

  @Override
  SourceLocation location() {
    return location;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("raise ");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RaiseStatement that = (RaiseStatement) o;
    return Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statement, location);
  }
}

final class CallStatement extends Statement {

  Expr expr;

  CallStatement(Expr expr) {
    this.expr = expr;
  }

  @Override
  SourceLocation location() {
    return expr.location();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    expr.prettyPrint(indent, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CallStatement that = (CallStatement) o;
    return Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(expr);
  }
}

final class PlaceholderStatement extends Statement {

  List<String> segments;
  SyntaxType type;
  SourceLocation loc;

  PlaceholderStatement(List<String> segments, SyntaxType type, SourceLocation loc) {
    this.segments = segments;
    this.type = type;
    this.loc = loc;
  }

  @Override
  <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return type;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("$");
    builder.append(String.join(".", segments));
  }
}

/**
 * An internal temporary placeholder of macro instantiations.
 * This node should never leave the parser.
 */
final class MacroInstanceStatement extends Statement implements MacroInstance {
  MacroOrPlaceholder macro;
  List<Node> arguments;
  SourceLocation loc;

  public MacroInstanceStatement(MacroOrPlaceholder macro, List<Node> arguments,
                                SourceLocation loc) {
    this.macro = macro;
    this.arguments = arguments;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return macro.returnType();
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
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
  public MacroOrPlaceholder macroOrPlaceholder() {
    return macro;
  }
}

/**
 * An internal temporary placeholder of a macro-level "match" construct.
 * This node should never leave the parser.
 */
final class MacroMatchStatement extends Statement {
  MacroMatch macroMatch;

  MacroMatchStatement(MacroMatch macroMatch) {
    this.macroMatch = macroMatch;
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
  public void prettyPrint(int indent, StringBuilder builder) {
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

    MacroMatchStatement that = (MacroMatchStatement) o;
    return macroMatch.equals(that.macroMatch);
  }

  @Override
  public int hashCode() {
    return macroMatch.hashCode();
  }
}

final class MatchStatement extends Statement {
  Expr candidate;
  List<Case> cases;
  @Nullable
  Statement defaultResult;
  SourceLocation loc;

  MatchStatement(Expr candidate, List<Case> cases, @Nullable Statement defaultResult,
                 SourceLocation loc) {
    this.candidate = candidate;
    this.cases = cases;
    this.defaultResult = defaultResult;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append("match ");
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
    if (defaultResult != null) {
      builder.append(prettyIndentString(indent + 1)).append(", _ => ");
      defaultResult.prettyPrint(0, builder);
      builder.append("\n");
    }
    builder.append(prettyIndentString(indent + 1)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MatchStatement that = (MatchStatement) o;
    return Objects.equals(candidate, that.candidate)
        && Objects.equals(cases, that.cases)
        && Objects.equals(defaultResult, that.defaultResult);
  }

  @Override
  public int hashCode() {
    int result = cases.hashCode();
    result = 31 * result + cases.hashCode();
    result = 31 * result + Objects.hashCode(defaultResult);
    return result;
  }

  record Case(List<Expr> patterns, Statement result) {
  }
}

final class InstructionCallStatement extends Statement {

  IdentifierOrPlaceholder id;
  List<NamedArgument> namedArguments;
  List<Expr> unnamedArguments;
  SourceLocation loc;

  InstructionCallStatement(IdentifierOrPlaceholder id, List<NamedArgument> namedArguments,
                           List<Expr> unnamedArguments, SourceLocation loc) {
    this.id = id;
    this.namedArguments = namedArguments;
    this.unnamedArguments = unnamedArguments;
    this.loc = loc;
  }

  Identifier id() {
    return (Identifier) id;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    id.prettyPrint(0, builder);
    if (!namedArguments.isEmpty()) {
      builder.append("{");
      var isFirst = true;
      for (NamedArgument namedArgument : namedArguments) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        namedArgument.name.prettyPrint(0, builder);
        builder.append(" = ");
        namedArgument.value.prettyPrint(0, builder);
      }
      builder.append("}");
    }
    if (!unnamedArguments.isEmpty()) {
      builder.append("(");
      var isFirst = true;
      for (Expr arg : unnamedArguments) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        arg.prettyPrint(0, builder);
      }
      builder.append(")");
    }
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstructionCallStatement that = (InstructionCallStatement) o;
    return Objects.equals(id, that.id)
        && Objects.equals(namedArguments, that.namedArguments)
        && Objects.equals(unnamedArguments, that.unnamedArguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, namedArguments, unnamedArguments);
  }

  record NamedArgument(Identifier name, Expr value) {
  }
}

final class LockStatement extends Statement {
  Expr expr;
  Statement statement;
  SourceLocation loc;

  LockStatement(Expr expr, Statement statement, SourceLocation loc) {
    this.expr = expr;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("lock ");
    expr.prettyPrint(0, builder);
    builder.append(" in\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LockStatement that = (LockStatement) o;
    return Objects.equals(expr, that.expr)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expr, statement);
  }
}

final class ForallStatement extends Statement {
  List<Index> indices;
  Statement statement;
  SourceLocation loc;

  ForallStatement(List<Index> indices, Statement statement, SourceLocation loc) {
    this.indices = indices;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  SourceLocation location() {
    return loc;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("forall ");
    var isFirst = true;
    for (Index index : indices) {
      if (!isFirst) {
        builder.append(", ");
      }
      index.name.prettyPrint(0, builder);
      builder.append(" in ");
      index.domain.prettyPrint(0, builder);
    }
    builder.append(" do\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForallStatement that = (ForallStatement) o;
    return Objects.equals(indices, that.indices)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indices, statement);
  }

  record Index(Identifier name, Expr domain) {
  }
}

