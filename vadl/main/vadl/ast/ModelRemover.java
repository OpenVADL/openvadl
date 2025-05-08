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
 * Removes all model definitions in the AST.
 * Model definitions are needed in the AST for ISA inheritance and across module imports, but can be
 * removed after macro expansion. This is especially useful for testing, where two AST trees are
 * often tested for semantic equality and thus stripped of models before comparison.
 */
public class ModelRemover implements DefinitionVisitor<Definition> {

  /**
   * Remove all models in the ast.
   *
   * @param ast to be modified.
   */
  public void removeModels(Ast ast) {
    var startTime = System.nanoTime();
    ast.definitions.removeIf(this::shouldRemove);
    ast.definitions.replaceAll(definition -> definition.accept(this));
    ast.passTimings.add(
        new Ast.PassTimings("Model Removing", (System.nanoTime() - startTime) / 1_000_000));
  }

  @Override
  public Definition visit(ConstantDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(FormatDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(DerivedFormatField definition) {
    return definition;
  }

  @Override
  public Definition visit(RangeFormatField definition) {
    return definition;
  }

  @Override
  public Definition visit(TypedFormatField definition) {
    return definition;
  }

  @Override
  public Definition visit(InstructionSetDefinition definition) {
    definition.definitions.removeIf(this::shouldRemove);
    return definition;
  }

  @Override
  public Definition visit(CounterDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MemoryDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RegisterDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RegisterFileDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(InstructionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(PseudoInstructionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(RelocationDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(EncodingDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AssemblyDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(UsingDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AbiClangTypeDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AbiClangNumericTypeDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AbiPseudoInstructionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(FunctionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AliasDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AnnotationDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(EnumerationDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(ExceptionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(PlaceholderDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MacroInstanceDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MacroMatchDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(DefinitionList definition) {
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
    removeModels(importDefinition.moduleAst);
    return importDefinition;
  }

  @Override
  public Definition visit(ProcessDefinition processDefinition) {
    return processDefinition;
  }

  @Override
  public Definition visit(OperationDefinition operationDefinition) {
    return operationDefinition;
  }

  @Override
  public Definition visit(Parameter definition) {
    return definition;
  }

  @Override
  public Definition visit(GroupDefinition groupDefinition) {
    return groupDefinition;
  }

  @Override
  public Definition visit(ApplicationBinaryInterfaceDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AbiSequenceDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(SpecialPurposeRegisterDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(ProcessorDefinition definition) {
    definition.definitions.removeIf(this::shouldRemove);
    definition.definitions.replaceAll(def -> def.accept(this));
    return definition;
  }

  @Override
  public Definition visit(PatchDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(SourceDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(CpuFunctionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(CpuMemoryRegionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(CpuProcessDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(MicroArchitectureDefinition definition) {
    definition.definitions.replaceAll(def -> def.accept(this));
    definition.definitions.removeIf(this::shouldRemove);
    return definition;
  }

  @Override
  public Definition visit(MacroInstructionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(PortBehaviorDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(PipelineDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(StageDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(CacheDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(LogicDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(SignalDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmDescriptionDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmModifierDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmDirectiveDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmGrammarRuleDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmGrammarAlternativesDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmGrammarElementDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmGrammarLocalVarDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmGrammarLiteralDefinition definition) {
    return definition;
  }

  @Override
  public Definition visit(AsmGrammarTypeDefinition definition) {
    return definition;
  }

  private boolean shouldRemove(Definition definition) {
    return definition instanceof ModelDefinition
        || definition instanceof RecordTypeDefinition
        || definition instanceof ModelTypeDefinition;
  }
}
