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


interface AstVisitor<T> extends DefinitionVisitor<T>, StatementVisitor<T>, ExprVisitor<T> {

}

/**
 * A {@link AstVisitor} that traverses the Ast on it's own and provides methods that get executed
 * on every node.
 *
 * <p>Note: This is a class instead of an interface because if new nodes get added to the AST this
 * forces the implementer to add the necessary methods in the AstVisitor and in this class.
 */
class RecursiveAstVisitor implements AstVisitor<Void> {
  protected void beforeTravel(Expr expr) {
  }

  protected void beforeTravel(Statement statement) {
  }

  protected void beforeTravel(Definition definition) {
  }

  protected void afterTravel(Statement statement) {
  }

  protected void afterTravel(Definition definition) {
  }

  protected void afterTravel(Expr expr) {
  }


  protected final void travel(Node node) {
    if (node instanceof Expr expr) {
      expr.accept(this);
      return;
    }
    if (node instanceof Statement stmt) {
      stmt.accept(this);
      return;
    }
    if (node instanceof Definition def) {
      def.accept(this);
      return;
    }

    // If it's just a intermediate node, just visit it's children
    node.children().forEach(this::travel);
  }

  @Override
  public Void visit(AbiSequenceDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AliasDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(ApplicationBinaryInterfaceDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmDescriptionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmDirectiveDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarAlternativesDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarElementDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarLiteralDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarLocalVarDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarRuleDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmGrammarTypeDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AsmModifierDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AssemblyDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(CacheDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(ConstantDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(CounterDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(CpuFunctionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(CpuProcessDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(DefinitionList definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(EncodingDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(EnumerationDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(ExceptionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(FormatDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(DerivedFormatField definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(RangeFormatField definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(TypedFormatField definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(FunctionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(GroupDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(ImportDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(InstructionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(InstructionSetDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(LogicDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstanceDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(MacroInstructionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(MacroMatchDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(MemoryDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(MicroArchitectureDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(MicroProcessorDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(ModelDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(ModelTypeDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(OperationDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(Parameter definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(PatchDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(PipelineDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(PlaceholderDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(PortBehaviorDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(ProcessDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(PseudoInstructionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(RecordTypeDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(RegisterDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(RegisterFileDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(RelocationDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(SignalDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(SourceDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(SpecialPurposeRegisterDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(StageDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(UsingDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AbiClangTypeDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AbiClangNumericTypeDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(AbiPseudoInstructionDefinition definition) {
    beforeTravel(definition);
    definition.children().forEach(this::travel);
    afterTravel(definition);
    return null;
  }

  @Override
  public Void visit(Identifier expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(BinaryExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(GroupedExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(IntegerLiteral expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(BinaryLiteral expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(BoolLiteral expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(StringLiteral expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(PlaceholderExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(MacroInstanceExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(RangeExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(TypeLiteral expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(IdentifierPath expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(UnaryExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(CallIndexExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(IfExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(LetExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(CastExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(SymbolExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(MacroMatchExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(MatchExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(ExtendIdExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(IdToStrExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(ExistsInExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(ExistsInThenExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(ForallThenExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(ForallExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(SequenceCallExpr expr) {
    beforeTravel(expr);
    expr.children().forEach(this::travel);
    afterTravel(expr);
    return null;
  }

  @Override
  public Void visit(AssignmentStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(BlockStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(CallStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(ForallStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(IfStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(InstructionCallStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(LetStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(LockStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(MacroInstanceStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(MacroMatchStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(MatchStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(PlaceholderStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(RaiseStatement statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }

  @Override
  public Void visit(StatementList statement) {
    beforeTravel(statement);
    statement.children().forEach(this::travel);
    afterTravel(statement);
    return null;
  }
}