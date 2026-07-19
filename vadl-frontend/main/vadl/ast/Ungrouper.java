// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import vadl.ast.nodes.AbiClangNumericTypeDefinition;
import vadl.ast.nodes.AbiClangTypeDefinition;
import vadl.ast.nodes.AbiSequenceDefinition;
import vadl.ast.nodes.AbiSpecialPurposeInstructionDefinition;
import vadl.ast.nodes.AliasDefinition;
import vadl.ast.nodes.AnnotationDefinition;
import vadl.ast.nodes.ApplicationBinaryInterfaceDefinition;
import vadl.ast.nodes.AsIdExpr;
import vadl.ast.nodes.AsStrExpr;
import vadl.ast.nodes.AsmDescriptionDefinition;
import vadl.ast.nodes.AsmDirectiveDefinition;
import vadl.ast.nodes.AsmGrammarAlternativesDefinition;
import vadl.ast.nodes.AsmGrammarElementDefinition;
import vadl.ast.nodes.AsmGrammarLiteralDefinition;
import vadl.ast.nodes.AsmGrammarLocalVarDefinition;
import vadl.ast.nodes.AsmGrammarRuleDefinition;
import vadl.ast.nodes.AsmGrammarTypeDefinition;
import vadl.ast.nodes.AsmModifierDefinition;
import vadl.ast.nodes.AssemblyDefinition;
import vadl.ast.nodes.AssignmentStatement;
import vadl.ast.nodes.BinaryExpr;
import vadl.ast.nodes.BinaryLiteral;
import vadl.ast.nodes.BlockStatement;
import vadl.ast.nodes.BoolLiteral;
import vadl.ast.nodes.CacheDefinition;
import vadl.ast.nodes.CallIndexExpr;
import vadl.ast.nodes.CallStatement;
import vadl.ast.nodes.CastExpr;
import vadl.ast.nodes.ConstantDefinition;
import vadl.ast.nodes.CounterDefinition;
import vadl.ast.nodes.CpuFunctionDefinition;
import vadl.ast.nodes.CpuMemoryRegionDefinition;
import vadl.ast.nodes.CpuProcessDefinition;
import vadl.ast.nodes.Definition;
import vadl.ast.nodes.DefinitionList;
import vadl.ast.nodes.DefinitionVisitor;
import vadl.ast.nodes.DerivedFormatField;
import vadl.ast.nodes.EncodingDefinition;
import vadl.ast.nodes.EncodingFormatField;
import vadl.ast.nodes.EnumerationDefinition;
import vadl.ast.nodes.ExceptionDefinition;
import vadl.ast.nodes.ExistsInExpr;
import vadl.ast.nodes.ExistsInThenExpr;
import vadl.ast.nodes.ExpandedAliasDefSequenceCallExpr;
import vadl.ast.nodes.ExpandedSequenceCallExpr;
import vadl.ast.nodes.Expr;
import vadl.ast.nodes.ExprVisitor;
import vadl.ast.nodes.FloatTypeDefinition;
import vadl.ast.nodes.ForallExpr;
import vadl.ast.nodes.ForallStatement;
import vadl.ast.nodes.ForallThenExpr;
import vadl.ast.nodes.FormatDefinition;
import vadl.ast.nodes.FunctionDefinition;
import vadl.ast.nodes.GroupDefinition;
import vadl.ast.nodes.GroupedExpr;
import vadl.ast.nodes.Identifier;
import vadl.ast.nodes.IdentifierPath;
import vadl.ast.nodes.IfExpr;
import vadl.ast.nodes.IfStatement;
import vadl.ast.nodes.ImportDefinition;
import vadl.ast.nodes.InstructionCallStatement;
import vadl.ast.nodes.InstructionDefinition;
import vadl.ast.nodes.InstructionSetDefinition;
import vadl.ast.nodes.IntegerLiteral;
import vadl.ast.nodes.IsSymExpr;
import vadl.ast.nodes.LetExpr;
import vadl.ast.nodes.LetStatement;
import vadl.ast.nodes.LockStatement;
import vadl.ast.nodes.LogicDefinition;
import vadl.ast.nodes.MacroInstanceDefinition;
import vadl.ast.nodes.MacroInstanceExpr;
import vadl.ast.nodes.MacroInstanceStatement;
import vadl.ast.nodes.MacroInstructionDefinition;
import vadl.ast.nodes.MacroMatchDefinition;
import vadl.ast.nodes.MacroMatchExpr;
import vadl.ast.nodes.MacroMatchStatement;
import vadl.ast.nodes.MatchExpr;
import vadl.ast.nodes.MatchStatement;
import vadl.ast.nodes.MemoryDefinition;
import vadl.ast.nodes.MicroArchitectureDefinition;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.OperationDefinition;
import vadl.ast.nodes.Parameter;
import vadl.ast.nodes.PatchDefinition;
import vadl.ast.nodes.PipelineDefinition;
import vadl.ast.nodes.PlaceholderDefinition;
import vadl.ast.nodes.PlaceholderExpr;
import vadl.ast.nodes.PlaceholderStatement;
import vadl.ast.nodes.PortBehaviorDefinition;
import vadl.ast.nodes.PredicateFormatField;
import vadl.ast.nodes.ProcessDefinition;
import vadl.ast.nodes.ProcessorDefinition;
import vadl.ast.nodes.PseudoInstructionDefinition;
import vadl.ast.nodes.RaiseStatement;
import vadl.ast.nodes.RangeExpr;
import vadl.ast.nodes.RangeFormatField;
import vadl.ast.nodes.RecordTypeDefinition;
import vadl.ast.nodes.RegisterDefinition;
import vadl.ast.nodes.RelocationDefinition;
import vadl.ast.nodes.ResourceReferenceExression;
import vadl.ast.nodes.SequenceCallExpr;
import vadl.ast.nodes.SignalDefinition;
import vadl.ast.nodes.SourceDefinition;
import vadl.ast.nodes.SpecialPurposeRegisterDefinition;
import vadl.ast.nodes.StageDefinition;
import vadl.ast.nodes.StageOutputDefinition;
import vadl.ast.nodes.StatementList;
import vadl.ast.nodes.StatementVisitor;
import vadl.ast.nodes.StringLiteral;
import vadl.ast.nodes.SymbolExpr;
import vadl.ast.nodes.TypeLiteral;
import vadl.ast.nodes.TypedFormatField;
import vadl.ast.nodes.UnaryExpr;
import vadl.ast.nodes.UsingDefinition;
import vadl.ast.nodes.WildcardLiteral;

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
  public static void ungroup(Ast ast) {
    ast.withPassTiming("Ungrouping", () -> {
      var ungrouper = new Ungrouper();
      for (var def : ast.definitions) {
        def.accept(ungrouper);
      }
    });
  }

  private Ungrouper() {
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
  public Expr visit(WildcardLiteral expr) {
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
  public Expr visit(CallIndexExpr expr) {
    expr.target = (IsSymExpr) ((Expr) expr.target).accept(this);
    for (int i = 0; i < expr.argsIndices.size(); i++) {
      var entry = expr.argsIndices.get(i);
      entry.values.replaceAll(e -> e.accept(this));
    }

    for (int i = 0; i < expr.subCalls.size(); i++) {
      var subCall = expr.subCalls.get(i);
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
    for (var matchCase : expr.cases) {
      matchCase.patterns.replaceAll(pattern -> pattern.accept(this));
      matchCase.result = matchCase.result.accept(this);
    }
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
    expr.thenExpr = expr.thenExpr.accept(this);
    return expr;
  }

  @Override
  public Expr visit(ForallThenExpr expr) {
    expr.thenExpr = expr.thenExpr.accept(this);
    return expr;
  }

  @Override
  public Expr visit(ForallExpr expr) {
    for (var index : expr.indices) {
      index.domain = index.domain.accept(this);
    }
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
  public Expr visit(ResourceReferenceExression expr) {
    return expr;
  }

  @Override
  public Void visit(FloatTypeDefinition definition) {
    ungroupAnnotations(definition);
    return null;
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
    for (var f : definition.fields) {
      f.accept(this);
    }
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
  public Void visit(EncodingFormatField definition) {
    definition.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(PredicateFormatField definition) {
    definition.expr.accept(this);
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    ungroupAnnotations(definition);
    for (var d : definition.definitions) {
      d.accept(this);
    }
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
    for (var stmt : definition.statements) {
      stmt.accept(this);
    }
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
    for (var encoding : definition.encodings.items) {
      var enc = (EncodingDefinition.EncodingField) encoding;
      enc.value = enc.value.accept(this);
    }
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
    for (var templateParam : processDefinition.templateParams) {
      templateParam.value =
          templateParam.value == null ? null : templateParam.value.accept(this);
    }
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
    return null;
  }

  @Override
  public Void visit(StageOutputDefinition definition) {
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
    for (var stmt : definition.statements) {
      stmt.accept(this);
    }
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
    for (var def : definition.definitions) {
      def.accept(this);
    }
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
    for (var def : definition.definitions) {
      def.accept(this);
    }
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
    for (var statement : blockStatement.statements) {
      statement.accept(this);
    }
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
    for (var matchCase : matchStatement.cases) {
      matchCase.patterns.replaceAll(pattern -> pattern.accept(this));
      matchCase.result.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(StatementList statementList) {
    for (var stmt : statementList.items) {
      stmt.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(InstructionCallStatement instructionCallStatement) {
    for (var namedArgument : instructionCallStatement.namedArguments) {
      namedArgument.value = namedArgument.value.accept(this);
    }
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
    for (var index : forallStatement.indices) {
      index.domain = index.domain.accept(this);
    }
    forallStatement.body.accept(this);
    return null;
  }

  private void ungroupAnnotations(Definition definition) {
    for (var i = 0; i < definition.annotations.size(); i++) {
      var annotation = definition.annotations.get(i);
      visit(annotation);
    }
  }
}
