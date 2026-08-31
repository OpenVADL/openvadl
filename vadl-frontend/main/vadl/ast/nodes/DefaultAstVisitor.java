// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.ast.nodes;

/**
 * A AST visitor that provides default methods that are called as a fallback/catch-all.
 * For visitors where almost every element get's handled the same this makes it much easier
 * to implement the common case once and only point out the out of ordinaries.
 *
 * <p>By default the visitNode method is always called but it is possible to also implement handling
 * for all definitions/statements/expressions by overriding visitDefinition or custom handling by
 * overriding the explicit visiting method.
 */
public abstract class DefaultAstVisitor<T> implements AstVisitor<T> {

  public abstract T visitNode(Node node);

  public T visitDefinition(Definition definition) {
    return visitNode(definition);
  }

  public T visitStatement(Statement statement) {
    return visitNode(statement);
  }

  public T visitExpression(Expr expression) {
    return visitNode(expression);
  }


  @Override
  public T visit(AbiSequenceDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AliasDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AnnotationDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ApplicationBinaryInterfaceDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmDescriptionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmDirectiveDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmGrammarAlternativesDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmGrammarElementDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmGrammarLiteralDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmGrammarLocalVarDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmGrammarRuleDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmGrammarTypeDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AsmModifierDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AssemblyDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(CacheDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ConstantDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(CounterDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(CpuFunctionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(CpuMemoryRegionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(CpuProcessDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(DefinitionList definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(EncodingDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(EnumerationDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ExceptionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(FloatTypeDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(FormatDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(DerivedFormatField definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(RangeFormatField definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(TypedFormatField definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(EncodingFormatField definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(PredicateFormatField definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(FunctionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(GroupDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ImportDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(InstructionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(InstructionSetDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(LogicDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(MacroInstanceDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(MacroInstructionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(MacroMatchDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(MemoryDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(MicroArchitectureDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ProcessorDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ModelDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ModelTypeDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(OperationDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(Parameter definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(PatchDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(PipelineDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(PlaceholderDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(PortBehaviorDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(ProcessDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(PseudoInstructionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(RecordTypeDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(RegisterDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(RelocationDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(SignalDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(SourceDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(SpecialPurposeRegisterDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(StageDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(UsingDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AbiClangTypeDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AbiClangNumericTypeDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(StageOutputDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(AbiSpecialPurposeInstructionDefinition definition) {
    return visitDefinition(definition);
  }

  @Override
  public T visit(Identifier expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(BinaryExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(GroupedExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(IntegerLiteral expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(WildcardLiteral expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(BinaryLiteral expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(BoolLiteral expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(StringLiteral expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(PlaceholderExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(MacroInstanceExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(RangeExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(TypeLiteral expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(IdentifierPath expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(UnaryExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(CallIndexExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(IfExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(LetExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(CastExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(SymbolExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(MacroMatchExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(MatchExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(AsIdExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(AsStrExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(ExistsInExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(ExistsInThenExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(ForallThenExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(ForallExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(SequenceCallExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(ExpandedSequenceCallExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(ExpandedAliasDefSequenceCallExpr expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(ResourceReferenceExression expr) {
    return visitExpression(expr);
  }

  @Override
  public T visit(AssignmentStatement statement) {
    return visitStatement(statement);
  }

  @Override
  public T visit(BlockStatement statement) {

    return visitStatement(statement);
  }

  @Override
  public T visit(CallStatement statement) {

    return visitStatement(statement);
  }

  @Override
  public T visit(ForallStatement statement) {

    return visitStatement(statement);
  }

  @Override
  public T visit(IfStatement statement) {

    return visitStatement(statement);
  }

  @Override
  public T visit(InstructionCallStatement statement) {

    return visitStatement(statement);
  }

  @Override
  public T visit(NewLabelStatement statement) {

    return visitStatement(statement);
  }

  @Override
  public T visit(LetStatement statement) {

    return visitStatement(statement);
  }

  @Override
  public T visit(LockStatement statement) {
    return visitStatement(statement);

  }

  @Override
  public T visit(MacroInstanceStatement statement) {
    return visitStatement(statement);

  }

  @Override
  public T visit(MacroMatchStatement statement) {
    return visitStatement(statement);

  }

  @Override
  public T visit(MatchStatement statement) {
    return visitStatement(statement);

  }

  @Override
  public T visit(PlaceholderStatement statement) {
    return visitStatement(statement);

  }

  @Override
  public T visit(RaiseStatement statement) {
    return visitStatement(statement);

  }

  @Override
  public T visit(StatementList statement) {
    return visitStatement(statement);

  }

}
