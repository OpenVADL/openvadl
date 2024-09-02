package vadl.ast;

import java.util.ArrayList;

/**
 * Removes all group expressions recursively.
 * Groups are needed in the AST during parsing until all binary expressions are reordered, but can
 * then be removed. This is especially useful for testing, where two AST trees are often tested
 * for semantic equality and thus ungrouped before comparison.
 */
class Ungrouper
    implements ExprVisitor<Expr>, DefinitionVisitor<Definition>, StatementVisitor<Statement> {

  public void ungroup(Ast ast) {
    ast.definitions.replaceAll(definition -> definition.accept(this));
  }

  @Override
  public Expr visit(Identifier expr) {
    return expr;
  }

  @Override
  public Expr visit(BinaryExpr expr) {
    expr.left = expr.left.accept(this);
    expr.right = expr.right.accept(this);
    return expr;
  }

  @Override
  public Expr visit(GroupedExpr expr) {
    if (expr.expressions.size() == 1) {
      return expr.expressions.get(0).accept(this);
    }
    var expressions = new ArrayList<>(expr.expressions);
    expressions.replaceAll(e -> e.accept(this));
    return new GroupedExpr(expressions, expr.loc);
  }

  @Override
  public Expr visit(IntegerLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(BinaryLiteral expr) {
    return expr;
  }

  @Override
  public Expr visit(BoolLiteral expr) {
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

  @Override
  public Expr visit(MacroMatchExpr expr) {
    return expr;
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    ungroupAnnotations(definition);
    definition.value = definition.value.accept(this);
    return definition;
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    ungroupAnnotations(definition);
    for (FormatDefinition.FormatField field : definition.fields) {
      if (field instanceof FormatDefinition.RangeFormatField f) {
        f.ranges.replaceAll(range -> range.accept(this));
      } else if (field instanceof FormatDefinition.DerivedFormatField f) {
        f.expr = f.expr.accept(this);
      }
    }
    return definition;
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    ungroupAnnotations(definition);
    definition.definitions.replaceAll(d -> d.accept(this));
    return definition;
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    ungroupAnnotations(definition);
    definition.behavior = definition.behavior.accept(this);
    return definition;
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    ungroupAnnotations(definition);
    definition.fieldEncodings().encodings.replaceAll(
        encoding -> new EncodingDefinition.FieldEncoding(encoding.field(),
            encoding.value().accept(this)));
    return definition;
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    ungroupAnnotations(definition);
    definition.expr = definition.expr.accept(this);
    return definition;
  }

  @Override
  public Definition visit(PlaceholderDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(MacroInstanceDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(MacroMatchDefinition definition) {
    return definition;
  }

  @Override
  public Statement visit(BlockStatement blockStatement) {
    blockStatement.statements.replaceAll(statement -> statement.accept(this));
    return blockStatement;
  }

  @Override
  public Statement visit(LetStatement letStatement) {
    letStatement.valueExpression = letStatement.valueExpression.accept(this);
    letStatement.body = letStatement.body.accept(this);
    return letStatement;
  }

  @Override
  public Statement visit(IfStatement ifStatement) {
    ifStatement.condition = ifStatement.condition.accept(this);
    ifStatement.thenStmt = ifStatement.thenStmt.accept(this);
    ifStatement.elseStmt = ifStatement.elseStmt == null ? null : ifStatement.elseStmt.accept(this);
    return ifStatement;
  }

  @Override
  public Statement visit(AssignmentStatement assignmentStatement) {
    assignmentStatement.target = assignmentStatement.target.accept(this);
    assignmentStatement.valueExpression = assignmentStatement.valueExpression.accept(this);
    return assignmentStatement;
  }

  @Override
  public Statement visit(PlaceholderStatement placeholderStatement) {
    return placeholderStatement;
  }

  @Override
  public Statement visit(MacroInstanceStatement macroInstanceStatement) {
    return macroInstanceStatement;
  }

  @Override
  public Statement visit(MacroMatchStatement macroMatchStatement) {
    return macroMatchStatement;
  }

  private void ungroupAnnotations(Definition definition) {
    definition.annotations.annotations().replaceAll(
        annotation -> new Annotation(annotation.expr().accept(this), annotation.type(),
            annotation.property()));
  }
}
