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
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.GcbExpandPseudoInstructionCppFunction;
import vadl.lcb.passes.pseudo.AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass;
import vadl.lcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.AbiSequencesProvider;
import vadl.lcb.template.utils.PseudoInstructionProvider;
import vadl.pass.PassResults;
import vadl.viam.CompilerInstruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This file includes the definitions for expanding instructions in the MC layer.
 */
public class EmitMCInstExpanderHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitMCInstExpanderHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCInstExpander.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().targetName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/" + processorName
        + "MCInstExpander.h";
  }

  record RenderedCompilerInstruction(String header, CompilerInstruction compilerInstruction) {

  }

  /**
   * Get the simple names of the pseudo instructions.
   */
  private List<RenderedCompilerInstruction> pseudoInstructions(
      Specification specification,
      PassResults passResults,
      Map<PseudoInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions
  ) {
    return PseudoInstructionProvider.getSupportedPseudoInstructions(specification, passResults)
        .map(x -> new RenderedCompilerInstruction(
            ensureNonNull(cppFunctions.get(x), "cppFunction must exist")
                .functionName().lower(),
            x
        )).toList();
  }

  private List<RenderedCompilerInstruction> constantSequences(
      Specification specification,
      Map<CompilerInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions
  ) {
    return AbiSequencesProvider.constantSequences(specification)
        .map(x -> new RenderedCompilerInstruction(
            ensureNonNull(cppFunctions.get(x), "cppFunction must exist")
                .functionName().lower(),
            x
        )).toList();
  }


  private List<RenderedCompilerInstruction> registerAdjustmentSequences(
      Specification specification,
      Map<CompilerInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions
  ) {
    return AbiSequencesProvider.registerAdjustmentSequences(specification)
        .map(x -> new RenderedCompilerInstruction(
            ensureNonNull(cppFunctions.get(x), "cppFunction must exist")
                .functionName().lower(),
            x
        )).toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var cppFunctionsForPseudoInstructions =
        (IdentityHashMap<PseudoInstruction, GcbExpandPseudoInstructionCppFunction>)
            passResults.lastResultOf(
                PseudoExpansionFunctionGeneratorPass.class);
    var cppFunctionsForAbiConstantSequenceCompilerInstructions =
        (IdentityHashMap<CompilerInstruction, GcbExpandPseudoInstructionCppFunction>)
            passResults.lastResultOf(
                AbiSequencesCompilerInstructionExpansionFunctionGeneratorPass.class);

    var pseudoInstructions =
        pseudoInstructions(specification, passResults, cppFunctionsForPseudoInstructions);
    var constantSequences =
        constantSequences(specification, cppFunctionsForAbiConstantSequenceCompilerInstructions);
    var registerAdjustmentSequences =
        registerAdjustmentSequences(specification,
            cppFunctionsForAbiConstantSequenceCompilerInstructions);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(),
        "compilerInstructionHeaders",
        Stream.concat(
            pseudoInstructions.stream().map(e -> e.header).toList().stream(),
            Stream.concat(
                constantSequences.stream().map(e -> e.header).toList().stream(),
                registerAdjustmentSequences.stream().map(e -> e.header).toList().stream()
            )).toList()
    );
  }
}
