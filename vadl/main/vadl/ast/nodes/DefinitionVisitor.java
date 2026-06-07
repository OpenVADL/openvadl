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

@SuppressWarnings("MissingJavadocType")
public interface DefinitionVisitor<R> {
  R visit(AbiSpecialPurposeInstructionDefinition definition);

  R visit(AbiSequenceDefinition definition);

  R visit(AliasDefinition definition);

  R visit(AnnotationDefinition definition);

  R visit(ApplicationBinaryInterfaceDefinition definition);

  R visit(AsmDescriptionDefinition definition);

  R visit(AsmDirectiveDefinition definition);

  R visit(AsmGrammarAlternativesDefinition definition);

  R visit(AsmGrammarElementDefinition definition);

  R visit(AsmGrammarLiteralDefinition definition);

  R visit(AsmGrammarLocalVarDefinition definition);

  R visit(AsmGrammarRuleDefinition definition);

  R visit(AsmGrammarTypeDefinition definition);

  R visit(AsmModifierDefinition definition);

  R visit(AssemblyDefinition definition);

  R visit(CacheDefinition definition);

  R visit(ConstantDefinition definition);

  R visit(CounterDefinition definition);

  R visit(CpuFunctionDefinition definition);

  R visit(CpuMemoryRegionDefinition definition);

  R visit(CpuProcessDefinition definition);

  R visit(DefinitionList definition);

  R visit(EncodingDefinition definition);

  R visit(EnumerationDefinition definition);

  R visit(ExceptionDefinition definition);

  R visit(FormatDefinition definition);

  R visit(DerivedFormatField definition);

  R visit(RangeFormatField definition);

  R visit(TypedFormatField definition);

  R visit(EncodingFormatField definition);

  R visit(PredicateFormatField definition);

  R visit(FunctionDefinition definition);

  R visit(GroupDefinition definition);

  R visit(ImportDefinition definition);

  R visit(InstructionDefinition definition);

  R visit(InstructionSetDefinition definition);

  R visit(LogicDefinition definition);

  R visit(MacroInstanceDefinition definition);

  R visit(MacroInstructionDefinition definition);

  R visit(MacroMatchDefinition definition);

  R visit(MemoryDefinition definition);

  R visit(MicroArchitectureDefinition definition);

  R visit(ProcessorDefinition definition);

  R visit(ModelDefinition definition);

  R visit(ModelTypeDefinition definition);

  R visit(OperationDefinition definition);

  R visit(Parameter definition);

  R visit(PatchDefinition definition);

  R visit(PipelineDefinition definition);

  R visit(PlaceholderDefinition definition);

  R visit(PortBehaviorDefinition definition);

  R visit(ProcessDefinition definition);

  R visit(PseudoInstructionDefinition definition);

  R visit(RecordTypeDefinition definition);

  R visit(RegisterDefinition definition);

  R visit(RelocationDefinition definition);

  R visit(SignalDefinition definition);

  R visit(SourceDefinition definition);

  R visit(SpecialPurposeRegisterDefinition definition);

  R visit(StageDefinition definition);

  R visit(UsingDefinition definition);

  R visit(AbiClangTypeDefinition abiClangTypeDefinition);

  R visit(AbiClangNumericTypeDefinition abiClangNumericTypeDefinition);

  R visit(StageOutputDefinition stageOutputDefinition);
}
