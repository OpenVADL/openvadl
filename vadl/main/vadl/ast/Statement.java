// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.utils.WithSourceLocation;

abstract sealed class Statement extends Node
    permits AssignmentStatement, BlockStatement, CallStatement, ForallStatement,
    ForallStatement.Index, IfStatement, InstructionCallStatement, LetStatement, LockStatement,
    MacroInstanceStatement, MacroMatchStatement, MatchStatement, PlaceholderStatement,
    RaiseStatement, StatementList {
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
  T visit(AssignmentStatement statement);

  T visit(BlockStatement statement);

  T visit(CallStatement statement);

  T visit(ForallStatement statement);

  T visit(IfStatement statement);

  T visit(InstructionCallStatement statement);

  T visit(LetStatement statement);

  T visit(LockStatement statement);

  T visit(MacroInstanceStatement statement);

  T visit(MacroMatchStatement statement);

  T visit(MatchStatement statement);

  T visit(PlaceholderStatement statement);

  T visit(RaiseStatement statement);

  T visit(StatementList statement);
}

final class BlockStatement extends Statement {
  @Child
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

}

/**
 * If multiple identifiers are provided, they are used to unpack a tuple.
 */
final class LetStatement extends Statement {
  List<Identifier> identifiers;
  @Child
  Expr valueExpr;
  @Child
  Statement body;
  SourceLocation location;

  LetStatement(List<Identifier> identifiers, Expr valueExpr, Statement body,
               SourceLocation location) {
    this.identifiers = identifiers;
    this.valueExpr = valueExpr;
    this.body = body;
    this.location = location;
  }

  /**
   * Returns the index of one of the variables the statement defines.
   *
   * @return the type of the name provided.
   */
  int getIndexOf(String name) {
    return identifiers.stream().map(i -> i.name).toList().indexOf(name);
  }

  /**
   * Returns the type of one of the variables the statement defines.
   *
   * @return the type of the name provided.
   */
  Type getTypeOf(String name) {
    var valType = valueExpr.type;
    if (identifiers.size() == 1) {
      return Objects.requireNonNull(valType);
    }

    if (!(valType instanceof TupleType valTuple)) {
      throw new IllegalStateException("Expected TupleType but got " + valType);
    }

    return Objects.requireNonNull(valTuple.get(getIndexOf(name)));
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
    valueExpr.prettyPrint(indent + 1, builder);
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
        && Objects.equals(this.valueExpr, that.valueExpr)
        && Objects.equals(this.body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifiers, valueExpr, body);
  }


}

final class IfStatement extends Statement {
  @Child
  Expr condition;
  @Child
  Statement thenStmt;
  @Nullable
  @Child
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


}

final class AssignmentStatement extends Statement {
  @Child
  Expr target;
  @Child
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

}

final class StatementList extends Statement {

  @Child
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

  @Child
  Statement statement;
  SourceLocation location;

  @LazyInit
  String viamId;

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

  @Child
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
final class MacroInstanceStatement extends Statement implements IsMacroInstance {
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
final class MacroMatchStatement extends Statement implements IsMacroMatch {
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
  List<Node> children() {
    // This is too complicated for the @Child annotation
    var childNodes = new ArrayList<Node>();
    childNodes.add(candidate);
    cases.forEach(c -> {
      childNodes.addAll(c.patterns);
      childNodes.add(c.result);
    });
    childNodes.add(defaultResult);
    return childNodes;
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

  static final class Case implements WithSourceLocation {
    List<Expr> patterns;
    Statement result;

    Case(List<Expr> patterns, Statement result) {
      this.patterns = patterns;
      this.result = result;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Case) obj;
      return Objects.equals(this.patterns, that.patterns)
          && Objects.equals(this.result, that.result);
    }

    @Override
    public int hashCode() {
      return Objects.hash(patterns, result);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public SourceLocation sourceLocation() {
      return patterns.get(0).location().join(result.location());
    }
  }
}

final class InstructionCallStatement extends Statement {

  @Child
  IdentifierOrPlaceholder id;
  @Child
  List<NamedArgument> namedArguments;
  @Child
  List<Expr> unnamedArguments;
  SourceLocation loc;

  /**
   * The instruction or pseudo instruction to which it points.
   * Set by the symboltable.
   */
  @Nullable
  Definition instrDef;

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
    id.prettyPrint(indent, builder);
    if (!namedArguments.isEmpty()) {
      builder.append("{");
      var isFirst = true;
      for (NamedArgument namedArgument : namedArguments) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        namedArgument.prettyPrint(indent, builder);
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
        arg.prettyPrint(indent, builder);
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

  static final class NamedArgument extends Node {
    @Child
    Identifier name;
    @Child
    Expr value;

    NamedArgument(Identifier name, Expr value) {
      this.name = name;
      this.value = value;
    }


    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      name.prettyPrint(0, builder);
      builder.append(" = ");
      value.prettyPrint(0, builder);
    }

    @Override
    public SourceLocation location() {
      return name.location().join(value.location());
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (NamedArgument) obj;
      return Objects.equals(this.name, that.name)
          && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }
  }
}

final class LockStatement extends Statement {
  @Child
  Expr expr;
  @Child
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
  @Child
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
      index.prettyPrint(indent, builder);
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

  static final class Index extends Statement implements IdentifiableNode {
    Identifier name;
    Expr domain;

    public Index(Identifier name, Expr domain) {
      this.name = name;
      this.domain = domain;
    }

    @Override
    public Identifier identifier() {
      return name;
    }

    @Override
    SourceLocation location() {
      return name.location().join(domain.location());
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      name.prettyPrint(0, builder);
      builder.append(" in ");
      domain.prettyPrint(0, builder);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Index that = (Index) o;
      return name.equals(that.name) && domain.equals(that.domain);
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + domain.hashCode();
      return result;
    }
  }
}

