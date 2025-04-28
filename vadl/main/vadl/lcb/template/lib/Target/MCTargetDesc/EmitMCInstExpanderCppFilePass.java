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

package vadl.lcb.template.lib.Target.MCTargetDesc;

import static vadl.viam.ViamError.ensureNonNull;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppClassImplName;
import vadl.cppCodeGen.model.GcbExpandPseudoInstructionCppFunction;
import vadl.gcb.passes.IdentifyFieldUsagePass;
import vadl.gcb.passes.relocation.model.HasRelocationComputationAndUpdate;
import vadl.lcb.codegen.expansion.CompilerInstructionExpansionCodeGenerator;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.pseudo.AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass;
import vadl.lcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.AbiSequencesProvider;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
import vadl.lcb.template.utils.PseudoInstructionProvider;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.CompilerInstruction;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This file includes the implementations for expanding instructions in the MC layer.
 */
public class EmitMCInstExpanderCppFilePass extends LcbTemplateRenderingPass {

  public EmitMCInstExpanderCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCInstExpander.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCInstExpander.cpp";
  }

  record RenderedInstruction(CppClassImplName classImpl,
                             String header,
                             String code,
                             Identifier identifier) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "header", header,
          "code", code,
          "classImpl", classImpl,
          "compilerInstruction", Map.of(
              "name", identifier.simpleName()
          )
      );
    }
  }

  /**
   * Returns the pseudo instructions except the call and return pseudo instruction, which
   * is defined in the ABI. Actually, we only need to keep the CALL pseudo instruction out of
   * convenience. This has to do with {@code @plt} suffix / modifier when using PIC.
   */
  private List<RenderedInstruction> pseudoInstructions(
      Specification specification,
      Map<CompilerInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      List<HasRelocationComputationAndUpdate> relocations,
      PassResults passResults,
      GenerateLinkerComponentsPass.VariantKindStore variantKindStore,
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords) {
    return PseudoInstructionProvider.getSupportedPseudoInstructions(specification, passResults)
        .filter(x -> !isCallOrRet(specification, x))
        .map(pseudoInstruction -> renderPseudoInstruction(cppFunctions, fieldUsages,
            relocations,
            passResults,
            pseudoInstruction,
            variantKindStore,
            machineInstructionRecords))
        .toList();
  }

  /**
   * Return {@code true} when the given {@code pseudoInstruction} is {@code CALL} or {@code RET}.
   */
  private boolean isCallOrRet(Specification specification, PseudoInstruction pseudoInstruction) {
    var abi = specification.abi().orElseThrow();

    return pseudoInstruction == abi.callSequence()
        || pseudoInstruction == abi.returnSequence();
  }

  private List<RenderedInstruction> constantSequences(
      Specification specification,
      Map<CompilerInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      List<HasRelocationComputationAndUpdate> relocations,
      PassResults passResults,
      GenerateLinkerComponentsPass.VariantKindStore variantKindStore,
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords) {
    return AbiSequencesProvider.constantSequences(specification)
        .map(pseudoInstruction -> renderPseudoInstruction(cppFunctions, fieldUsages,
            relocations,
            passResults,
            pseudoInstruction,
            variantKindStore,
            machineInstructionRecords))
        .toList();
  }


  private List<RenderedInstruction> registerAdjustmentSequences(
      Specification specification,
      Map<CompilerInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      List<HasRelocationComputationAndUpdate> relocations,
      PassResults passResults,
      GenerateLinkerComponentsPass.VariantKindStore variantKindStore,
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords) {
    return AbiSequencesProvider.registerAdjustmentSequences(specification)
        .map(pseudoInstruction -> renderPseudoInstruction(cppFunctions, fieldUsages,
            relocations,
            passResults,
            pseudoInstruction,
            variantKindStore,
            machineInstructionRecords))
        .toList();
  }

  private @Nonnull RenderedInstruction renderPseudoInstruction(
      Map<CompilerInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      IdentifyFieldUsagePass.ImmediateDetectionContainer fieldUsages,
      List<HasRelocationComputationAndUpdate> relocations,
      PassResults passResults,
      CompilerInstruction compilerInstruction,
      GenerateLinkerComponentsPass.VariantKindStore variantKindStore,
      IdentityHashMap<Instruction, LlvmLoweringRecord.Machine> machineInstructionRecords) {
    var function = ensureNonNull(cppFunctions.get(compilerInstruction),
        "cpp function must exist)");

    var base = lcbConfiguration().targetName();
    var codeGen =
        new CompilerInstructionExpansionCodeGenerator(base,
            fieldUsages,
            ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults),
            relocations,
            variantKindStore,
            compilerInstruction,
            function,
            machineInstructionRecords);

    var renderedFunction = codeGen.genFunctionDefinition();
    var classPrefix = new CppClassImplName(
        lcbConfiguration().targetName().value().toLowerCase() + "MCInstExpander");
    ensureNonNull(function, "a function must exist");
    return new RenderedInstruction(
        classPrefix,
        compilerInstruction.identifier.lower() + "_" + function.identifier.simpleName(),
        renderedFunction,
        compilerInstruction.identifier);
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    IdentityHashMap<CompilerInstruction, GcbExpandPseudoInstructionCppFunction>
        cppFunctionsForPseudoInstructions =
        (IdentityHashMap<CompilerInstruction, GcbExpandPseudoInstructionCppFunction>)
            passResults.lastResultOf(
                PseudoExpansionFunctionGeneratorPass.class);
    var cppFunctionsForAbiSequences =
        (IdentityHashMap<CompilerInstruction, GcbExpandPseudoInstructionCppFunction>)
            passResults.lastResultOf(
                AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass.class);

    var fieldUsages = (IdentifyFieldUsagePass.ImmediateDetectionContainer) passResults.lastResultOf(
        IdentifyFieldUsagePass.class);
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var relocations = output.elfRelocations();
    var llvmLoweringPassResult =
        (LlvmLoweringPass.LlvmLoweringPassResult) passResults.lastResultOf(LlvmLoweringPass.class);
    var machineInstructionRecords = llvmLoweringPassResult.machineInstructionRecords();

    var pseudoInstructions =
        pseudoInstructions(specification, cppFunctionsForPseudoInstructions, fieldUsages,
            relocations,
            passResults, output.variantKindStore(), machineInstructionRecords);

    var constantSequences =
        constantSequences(specification, cppFunctionsForAbiSequences,
            fieldUsages, relocations, passResults,
            output.variantKindStore(), machineInstructionRecords);

    var registerAdjustmentSequences =
        registerAdjustmentSequences(specification, cppFunctionsForAbiSequences,
            fieldUsages, relocations, passResults,
            output.variantKindStore(), machineInstructionRecords);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "compilerInstructions", Stream.concat(pseudoInstructions.stream(),
                Stream.concat(constantSequences.stream(), registerAdjustmentSequences.stream()))
            .toList()
    );
  }
}
