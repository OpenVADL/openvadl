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

/**
 * Removes all group expressions recursively.
 * Groups are needed in the AST during parsing until all binary expressions are reordered, but can
 * then be removed. This is especially useful for testing, where two AST trees are often tested
 * for semantic equality and thus ungrouped before comparison.
 *
 * <p>NOTE: Do not replace any AST nodes here!
 * Because at this point in time, the symbol resolver already ran and if nodes get replaced the
 * SymbolTable will point to nodes no longer in the AST.
 * This will eventually fail when the typechecker assigns types to nodes in the AST but the
 * resolved symbols from the SymbolTable won't have any types.
 */
public class Ungrouper
    implements ExprVisitor<Expr>, DefinitionVisitor<Void>, StatementVisitor<Void> {

  /**
   * Remove all unneeded group expressions in the AST.
   *
   * @param ast to be modified.
   */
  public void ungroup(Ast ast) {
    var startTime = System.nanoTime();
    ast.definitions.forEach(def -> def.accept(this));
    ast.passTimings.add(
        new Ast.PassTimings("Ungrouping", (System.nanoTime() - startTime) / 1_000_000));
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
    expr.expressions.replaceAll(e -> e.accept(this));
    return expr;
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
  public Expr visit(TensorLiteral expr) {
    expr.children.replaceAll(e -> e.accept(this));
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
  public Expr visit(CallIndexExpr expr) {
    expr.target = (IsSymExpr) ((Expr) expr.target).accept(this);
    for (var entry : expr.argsIndices) {
      entry.values.replaceAll(e -> e.accept(this));
    }

    for (var subCall : expr.subCalls) {
      for (var entry : subCall.argsIndices) {
        entry.values.replaceAll(e -> e.accept(this));
      }
    }

    return expr;
  }

  @Override
  public Expr visit(IfExpr expr) {
    expr.condition = expr.condition.accept(this);
    expr.thenExpr = expr.thenExpr.accept(this);
    expr.elseExpr = expr.elseExpr.accept(this);
    return expr;
  }

  @Override
  public Expr visit(LetExpr expr) {
    expr.valueExpr = expr.valueExpr.accept(this);
    expr.body = expr.body.accept(this);
    return expr;
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
  public Expr visit(MacroMatchExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(MatchExpr expr) {
    expr.candidate = expr.candidate.accept(this);
    expr.defaultResult = expr.defaultResult.accept(this);
    expr.cases.forEach(matchCase -> {
      matchCase.patterns.replaceAll(pattern -> pattern.accept(this));
      matchCase.result = matchCase.result.accept(this);
    });
    return expr;
  }

  @Override
  public Expr visit(AsIdExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(AsStrExpr expr) {
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
  public Expr visit(ForallThenExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(ForallExpr expr) {
    expr.indices.forEach(
        index -> index.domain = index.domain.accept(this));
    expr.body = expr.body.accept(this);
    return expr;
  }

  @Override
  public Expr visit(SequenceCallExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(ExpandedSequenceCallExpr expr) {
    return expr;
  }

  @Override
  public Expr visit(ExpandedAliasDefSequenceCallExpr expr) {
    return expr;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    ungroupAnnotations(definition);
    definition.value = definition.value.accept(this);
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    ungroupAnnotations(definition);
    definition.fields.forEach(f -> f.accept(this));
    definition.auxiliaryFields.forEach(f -> f.accept(this));
    return null;
  }

  @Override
  public Void visit(DerivedFormatField definition) {
    ungroupAnnotations(definition);
    definition.expr = definition.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(RangeFormatField definition) {
    ungroupAnnotations(definition);
    if (definition.typeLiteral != null) {
      definition.typeLiteral = (TypeLiteral) definition.typeLiteral.accept(this);
    }
    definition.ranges = definition.ranges.stream().map(e -> e.accept(this)).toList();
    return null;
  }

  @Override
  public Void visit(TypedFormatField definition) {
    ungroupAnnotations(definition);
    definition.typeLiteral = (TypeLiteral) definition.typeLiteral.accept(this);
    return null;
  }

  @Override
  public Void visit(FormatDefinition.AuxiliaryField definition) {
    definition.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    ungroupAnnotations(definition);
    definition.definitions.forEach(d -> d.accept(this));
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    ungroupAnnotations(definition);
    definition.behavior.accept(this);
    return null;
  }

  @Override
  public Void visit(PseudoInstructionDefinition definition) {
    ungroupAnnotations(definition);
    definition.statements.forEach(this::visit);
    return null;
  }

  @Override
  public Void visit(RelocationDefinition definition) {
    definition.expr = definition.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    ungroupAnnotations(definition);
    definition.encodings.items.forEach(encoding -> {
      var enc = (EncodingDefinition.EncodingField) encoding;
      enc.value = enc.value.accept(this);
    });
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(AbiClangTypeDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(AbiClangNumericTypeDefinition definition) {
    ungroupAnnotations(definition);
    definition.size = definition.size.accept(this);
    return null;
  }

  @Override
  public Void visit(AbiSpecialPurposeInstructionDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    ungroupAnnotations(definition);
    definition.expr = definition.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(AliasDefinition definition) {
    ungroupAnnotations(definition);
    definition.value = definition.value.accept(this);
    return null;
  }

  @Override
  public Void visit(AnnotationDefinition definition) {
    definition.values.replaceAll(e -> e.accept(this));
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    ungroupAnnotations(definition);
    for (var entry : definition.entries) {
      if (entry.value != null) {
        entry.value = entry.value.accept(this);
      }
    }
    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstanceDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(MacroMatchDefinition definition) {
    return null;
  }

  @Override
  public Void visit(DefinitionList definition) {
    for (Definition item : definition.items) {
      item.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(ModelDefinition definition) {
    return null;
  }

  @Override
  public Void visit(RecordTypeDefinition definition) {
    return null;
  }

  @Override
  public Void visit(ModelTypeDefinition definition) {
    return null;
  }

  @Override
  public Void visit(ImportDefinition importDefinition) {
    ungroup(importDefinition.moduleAst);
    return null;
  }

  @Override
  public Void visit(ProcessDefinition processDefinition) {
    ungroupAnnotations(processDefinition);
    processDefinition.templateParams.forEach(
        templateParam ->
            templateParam.value =
                templateParam.value == null
                    ? null
                    : templateParam.value.accept(this));
    processDefinition.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(OperationDefinition operationDefinition) {
    ungroupAnnotations(operationDefinition);
    return null;
  }

  @Override
  public Void visit(Parameter definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(GroupDefinition groupDefinition) {
    ungroupAnnotations(groupDefinition);
    return null;
  }

  @Override
  public Void visit(ApplicationBinaryInterfaceDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(AbiSequenceDefinition definition) {
    ungroupAnnotations(definition);
    definition.statements.forEach(stmt -> stmt.accept(this));
    return null;
  }

  @Override
  public Void visit(SpecialPurposeRegisterDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(ProcessorDefinition definition) {
    ungroupAnnotations(definition);
    definition.definitions.forEach(def -> def.accept(this));
    return null;
  }

  @Override
  public Void visit(PatchDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(SourceDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(CpuFunctionDefinition definition) {
    ungroupAnnotations(definition);
    definition.expr = definition.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(CpuMemoryRegionDefinition definition) {
    ungroupAnnotations(definition);
    if (definition.stmt != null) {
      definition.stmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CpuProcessDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(MicroArchitectureDefinition definition) {
    ungroupAnnotations(definition);
    definition.definitions.forEach(def -> def.accept(this));
    return null;
  }

  @Override
  public Void visit(MacroInstructionDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(PortBehaviorDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(PipelineDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(StageDefinition definition) {
    ungroupAnnotations(definition);
    definition.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(CacheDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(LogicDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(SignalDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
    ungroupAnnotations(definition);
    return null;
  }

  @Override
  public Void visit(AsmModifierDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmDirectiveDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarElementDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarLocalVarDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition definition) {
    return null;
  }

  @Override
  public Void visit(AsmGrammarTypeDefinition definition) {
    return null;
  }

  @Override
  public Void visit(BlockStatement blockStatement) {
    blockStatement.statements.forEach(statement -> statement.accept(this));
    return null;
  }

  @Override
  public Void visit(LetStatement letStatement) {
    letStatement.valueExpr = letStatement.valueExpr.accept(this);
    letStatement.body.accept(this);
    return null;
  }

  @Override
  public Void visit(IfStatement ifStatement) {
    ifStatement.condition = ifStatement.condition.accept(this);
    ifStatement.thenStmt.accept(this);
    if (ifStatement.elseStmt != null) {
      ifStatement.elseStmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(AssignmentStatement assignmentStatement) {
    assignmentStatement.target = assignmentStatement.target.accept(this);
    assignmentStatement.valueExpression = assignmentStatement.valueExpression.accept(this);
    return null;
  }

  @Override
  public Void visit(RaiseStatement raiseStatement) {
    raiseStatement.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(CallStatement callStatement) {
    callStatement.expr = callStatement.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(PlaceholderStatement placeholderStatement) {
    return null;
  }

  @Override
  public Void visit(MacroInstanceStatement macroInstanceStatement) {
    return null;
  }

  @Override
  public Void visit(MacroMatchStatement macroMatchStatement) {
    return null;
  }

  @Override
  public Void visit(MatchStatement matchStatement) {
    matchStatement.candidate = matchStatement.candidate.accept(this);
    if (matchStatement.defaultResult != null) {
      matchStatement.defaultResult.accept(this);
    }
    matchStatement.cases.forEach(matchCase -> {
      matchCase.patterns.replaceAll(pattern -> pattern.accept(this));
      matchCase.result.accept(this);
    });
    return null;
  }

  @Override
  public Void visit(StatementList statementList) {
    statementList.items.forEach(stmt -> stmt.accept(this));
    return null;
  }

  @Override
  public Void visit(InstructionCallStatement instructionCallStatement) {
    instructionCallStatement.namedArguments.forEach(namedArgument ->
        namedArgument.value = namedArgument.value.accept(this)
    );
    instructionCallStatement.unnamedArguments.replaceAll(expr -> expr.accept(this));
    return null;
  }

  @Override
  public Void visit(LockStatement lockStatement) {
    lockStatement.expr = lockStatement.expr.accept(this);
    lockStatement.statement.accept(this);
    return null;
  }

  @Override
  public Void visit(ForallStatement forallStatement) {
    forallStatement.indices.forEach(index -> index.domain = index.domain.accept(this));
    forallStatement.body.accept(this);
    return null;
  }

  private void ungroupAnnotations(Definition definition) {
    definition.annotations.forEach(this::visit);
  }
}
