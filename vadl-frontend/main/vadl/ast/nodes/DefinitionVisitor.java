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

package vadl.ast.nodes;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public interface DefinitionVisitor<R> {
  public R visit(AbiSpecialPurposeInstructionDefinition definition);

  public R visit(AbiSequenceDefinition definition);

  public R visit(AliasDefinition definition);

  public R visit(AnnotationDefinition definition);

  public R visit(ApplicationBinaryInterfaceDefinition definition);

  public R visit(AsmDescriptionDefinition definition);

  public R visit(AsmDirectiveDefinition definition);

  public R visit(AsmGrammarAlternativesDefinition definition);

  public R visit(AsmGrammarElementDefinition definition);

  public R visit(AsmGrammarLiteralDefinition definition);

  public R visit(AsmGrammarLocalVarDefinition definition);

  public R visit(AsmGrammarRuleDefinition definition);

  public R visit(AsmGrammarTypeDefinition definition);

  public R visit(AsmModifierDefinition definition);

  public R visit(AssemblyDefinition definition);

  public R visit(CacheDefinition definition);

  public R visit(ConstantDefinition definition);

  public R visit(CounterDefinition definition);

  public R visit(CpuFunctionDefinition definition);

  public R visit(CpuMemoryRegionDefinition definition);

  public R visit(CpuProcessDefinition definition);

  public R visit(DefinitionList definition);

  public R visit(EncodingDefinition definition);

  public R visit(EnumerationDefinition definition);

  public R visit(ExceptionDefinition definition);

  public R visit(FormatDefinition definition);

  public R visit(DerivedFormatField definition);

  public R visit(RangeFormatField definition);

  public R visit(TypedFormatField definition);

  public R visit(EncodingFormatField definition);

  public R visit(PredicateFormatField definition);

  public R visit(FunctionDefinition definition);

  public R visit(GroupDefinition definition);

  public R visit(ImportDefinition definition);

  public R visit(InstructionDefinition definition);

  public R visit(InstructionSetDefinition definition);

  public R visit(LogicDefinition definition);

  public R visit(MacroInstanceDefinition definition);

  public R visit(MacroInstructionDefinition definition);

  public R visit(MacroMatchDefinition definition);

  public R visit(MemoryDefinition definition);

  public R visit(MicroArchitectureDefinition definition);

  public R visit(ProcessorDefinition definition);

  public R visit(ModelDefinition definition);

  public R visit(ModelTypeDefinition definition);

  public R visit(OperationDefinition definition);

  public R visit(Parameter definition);

  public R visit(PatchDefinition definition);

  public R visit(PipelineDefinition definition);

  public R visit(PlaceholderDefinition definition);

  public R visit(PortBehaviorDefinition definition);

  public R visit(ProcessDefinition definition);

  public R visit(PseudoInstructionDefinition definition);

  public R visit(RecordTypeDefinition definition);

  public R visit(RegisterDefinition definition);

  public R visit(RelocationDefinition definition);

  public R visit(SignalDefinition definition);

  public R visit(SourceDefinition definition);

  public R visit(SpecialPurposeRegisterDefinition definition);

  public R visit(StageDefinition definition);

  public R visit(UsingDefinition definition);

  public R visit(AbiClangTypeDefinition abiClangTypeDefinition);

  public R visit(AbiClangNumericTypeDefinition abiClangNumericTypeDefinition);

  public R visit(StageOutputDefinition stageOutputDefinition);
}
