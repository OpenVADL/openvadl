package vadl.ast;

import java.util.ArrayList;

/**
 * Removes all group expressions recursively.
 * Groups are needed in the AST during parsing until all binary expressions are reordered, but can
 * then be removed. This is especially useful for testing, where two AST trees are often tested
 * for semantic equality and thus ungrouped before comparison.
 */
public class Ungrouper
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
  public Expr visit(BinOpExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(UnOpExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(MacroMatchExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(MatchExpr expr) {
    expr.candidate = expr.candidate.accept(this);
    expr.defaultResult = expr.defaultResult.accept(this);
    expr.cases.replaceAll(matchCase -> {
      matchCase.patterns().replaceAll(pattern -> pattern.accept(this));
      return new MatchExpr.Case(matchCase.patterns(), matchCase.result().accept(this));
    });
    return expr;
  }

  @Override
  public Expr visit(ExtendIdExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(IdToStrExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(ExistsInExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(ExistsInThenExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(ForAllThenExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(SequenceCallExpr expr) {
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
    for (FormatDefinition.AuxiliaryField auxiliaryField : definition.auxiliaryFields) {
      auxiliaryField.entries().replaceAll(entry ->
          new FormatDefinition.AuxiliaryFieldEntry(entry.id(), entry.expr().accept(this)));
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
  public Definition visit(PseudoInstructionDefinition definition) {
    ungroupAnnotations(definition);
    definition.statements.replaceAll(this::visit);
    return definition;
  }

  @Override
  public Definition visit(RelocationDefinition definition) {
    definition.expr = definition.expr.accept(this);
    return definition;
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    ungroupAnnotations(definition);
    definition.fieldEncodings().encodings.replaceAll(encoding -> {
      var enc = (EncodingDefinition.FieldEncoding) encoding;
      return new EncodingDefinition.FieldEncoding(enc.field(), enc.value().accept(this));
    });
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
  public Definition visit(AliasDefinition definition) {
    ungroupAnnotations(definition);
    definition.value = definition.value.accept(this);
    return definition;
  }

  @Override
  public Definition visit(EnumerationDefinition definition) {
    ungroupAnnotations(definition);
    definition.entries.replaceAll(entry -> new EnumerationDefinition.Entry(entry.name(),
        entry.value() == null ? null : entry.value().accept(this),
        entry.behavior() == null ? null : entry.behavior().accept(this)));
    return definition;
  }

  @Override
  public Definition visit(ExceptionDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement = definition.statement.accept(this);
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
  public Definition visit(DefinitionList definition) {
    for (Definition item : definition.items) {
      item.accept(this);
    }
    return definition;
  }

  @Override
  public Definition visit(ModelDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RecordTypeDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(ModelTypeDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(ImportDefinition importDefinition) {
    return importDefinition;
  }

  @Override
  public Definition visit(ProcessDefinition processDefinition) {
    ungroupAnnotations(processDefinition);
    processDefinition.templateParams.replaceAll(
        templateParam -> new ProcessDefinition.TemplateParam(templateParam.name(),
            templateParam.type(),
            templateParam.value() == null ? null : templateParam.value().accept(this)));
    processDefinition.statement = processDefinition.statement.accept(this);
    return processDefinition;
  }

  @Override
  public Definition visit(OperationDefinition operationDefinition) {
    ungroupAnnotations(operationDefinition);
    return operationDefinition;
  }

  @Override
  public Definition visit(GroupDefinition groupDefinition) {
    ungroupAnnotations(groupDefinition);
    return groupDefinition;
  }

  @Override
  public Definition visit(ApplicationBinaryInterfaceDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(AbiSequenceDefinition definition) {
    ungroupAnnotations(definition);
    definition.statements.replaceAll(stmt -> (InstructionCallStatement) stmt.accept(this));
    return definition;
  }

  @Override
  public Definition visit(SpecialPurposeRegisterDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(MicroProcessorDefinition definition) {
    ungroupAnnotations(definition);
    definition.definitions.replaceAll(def -> def.accept(this));
    return definition;
  }

  @Override
  public Definition visit(PatchDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(SourceDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(CpuFunctionDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(CpuProcessDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement = definition.statement.accept(this);
    return definition;
  }

  @Override
  public Definition visit(MicroArchitectureDefinition definition) {
    ungroupAnnotations(definition);
    definition.definitions.replaceAll(def -> def.accept(this));
    return definition;
  }

  @Override
  public Definition visit(MacroInstructionDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement = definition.statement.accept(this);
    return definition;
  }

  @Override
  public Definition visit(PortBehaviorDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement = definition.statement.accept(this);
    return definition;
  }

  @Override
  public Definition visit(PipelineDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement = definition.statement.accept(this);
    return definition;
  }

  @Override
  public Definition visit(StageDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement = definition.statement.accept(this);
    return definition;
  }

  @Override
  public Definition visit(CacheDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(LogicDefinition definition) {
    ungroupAnnotations(definition);
    return definition;
  }

  @Override
  public Definition visit(SignalDefinition definition) {
    ungroupAnnotations(definition);
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
  public Statement visit(RaiseStatement raiseStatement) {
    raiseStatement.statement = raiseStatement.statement.accept(this);
    return raiseStatement;
  }

  @Override
  public Statement visit(CallStatement callStatement) {
    callStatement.expr = callStatement.expr.accept(this);
    return callStatement;
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

  @Override
  public Statement visit(MatchStatement matchStatement) {
    matchStatement.candidate = matchStatement.candidate.accept(this);
    matchStatement.defaultResult =
        matchStatement.defaultResult == null ? null : matchStatement.defaultResult.accept(this);
    matchStatement.cases.replaceAll(matchCase -> {
      matchCase.patterns().replaceAll(pattern -> pattern.accept(this));
      return new MatchStatement.Case(matchCase.patterns(), matchCase.result().accept(this));
    });
    return matchStatement;
  }

  @Override
  public Statement visit(StatementList statementList) {
    statementList.items.replaceAll(stmt -> stmt.accept(this));
    return statementList;
  }

  @Override
  public InstructionCallStatement visit(InstructionCallStatement instructionCallStatement) {
    instructionCallStatement.namedArguments.replaceAll(namedArgument ->
        new InstructionCallStatement.NamedArgument(namedArgument.name(),
            namedArgument.value().accept(this)));
    instructionCallStatement.unnamedArguments.replaceAll(expr -> expr.accept(this));
    return instructionCallStatement;
  }

  @Override
  public Statement visit(LockStatement lockStatement) {
    lockStatement.expr = lockStatement.expr.accept(this);
    lockStatement.statement = lockStatement.statement.accept(this);
    return lockStatement;
  }

  private void ungroupAnnotations(Definition definition) {
    definition.annotations.annotations().replaceAll(
        annotation -> new Annotation(annotation.expr().accept(this), annotation.type(),
            annotation.property()));
  }
}
