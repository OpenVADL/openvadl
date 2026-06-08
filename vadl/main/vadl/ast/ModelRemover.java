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
import vadl.ast.nodes.CacheDefinition;
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
import vadl.ast.nodes.FormatDefinition;
import vadl.ast.nodes.FunctionDefinition;
import vadl.ast.nodes.GroupDefinition;
import vadl.ast.nodes.ImportDefinition;
import vadl.ast.nodes.InstructionDefinition;
import vadl.ast.nodes.InstructionSetDefinition;
import vadl.ast.nodes.LogicDefinition;
import vadl.ast.nodes.MacroInstanceDefinition;
import vadl.ast.nodes.MacroInstructionDefinition;
import vadl.ast.nodes.MacroMatchDefinition;
import vadl.ast.nodes.MemoryDefinition;
import vadl.ast.nodes.MicroArchitectureDefinition;
import vadl.ast.nodes.ModelDefinition;
import vadl.ast.nodes.ModelTypeDefinition;
import vadl.ast.nodes.OperationDefinition;
import vadl.ast.nodes.Parameter;
import vadl.ast.nodes.PatchDefinition;
import vadl.ast.nodes.PipelineDefinition;
import vadl.ast.nodes.PlaceholderDefinition;
import vadl.ast.nodes.PortBehaviorDefinition;
import vadl.ast.nodes.PredicateFormatField;
import vadl.ast.nodes.ProcessDefinition;
import vadl.ast.nodes.ProcessorDefinition;
import vadl.ast.nodes.PseudoInstructionDefinition;
import vadl.ast.nodes.RangeFormatField;
import vadl.ast.nodes.RecordTypeDefinition;
import vadl.ast.nodes.RegisterDefinition;
import vadl.ast.nodes.RelocationDefinition;
import vadl.ast.nodes.SignalDefinition;
import vadl.ast.nodes.SourceDefinition;
import vadl.ast.nodes.SpecialPurposeRegisterDefinition;
import vadl.ast.nodes.StageDefinition;
import vadl.ast.nodes.StageOutputDefinition;
import vadl.ast.nodes.TypedFormatField;
import vadl.ast.nodes.UsingDefinition;

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
  public static void removeModels(Ast ast) {
    var remover = new ModelRemover();
    ast.withPassTiming("Model Removing", () -> {
      ast.definitions.removeIf(remover::shouldRemove);
      ast.definitions.replaceAll(definition -> definition.accept(remover));
    });
  }

  private ModelRemover() {
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
  public Definition visit(EncodingFormatField definition) {
    return definition;
  }

  @Override
  public Definition visit(PredicateFormatField definition) {
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
  public Definition visit(AbiSpecialPurposeInstructionDefinition definition) {
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
  public Definition visit(StageOutputDefinition definition) {
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
