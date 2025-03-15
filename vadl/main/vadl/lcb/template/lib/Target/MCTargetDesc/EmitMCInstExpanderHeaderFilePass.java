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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.GcbExpandPseudoInstructionCppFunction;
import vadl.lcb.passes.pseudo.PseudoExpansionFunctionGeneratorPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.PseudoInstructionProvider;
import vadl.pass.PassResults;
import vadl.viam.Abi;
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

  record RenderedPseudoInstruction(String header, PseudoInstruction pseudoInstruction) {

  }

  /**
   * Get the simple names of the pseudo instructions.
   */
  private List<RenderedPseudoInstruction> pseudoInstructions(
      Specification specification,
      PassResults passResults,
      Map<PseudoInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions
  ) {
    return PseudoInstructionProvider.getSupportedPseudoInstructions(specification, passResults)
        .map(x -> new RenderedPseudoInstruction(
            ensureNonNull(cppFunctions.get(x), "cppFunction must exist")
                .functionName().lower(),
            x
        )).toList();
  }

  private List<RenderedPseudoInstruction> compilerInstructions(
      Map<PseudoInstruction, GcbExpandPseudoInstructionCppFunction> cppFunctions,
      Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();

    return Stream.of(abi.returnSequence(), abi.callSequence())
        .map(x -> new RenderedPseudoInstruction(
            ensureNonNull(cppFunctions.get(x), "cppFunction must exist").identifier.lower(),
            x
        ))
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var cppFunctionsForPseudoInstructions =
        (IdentityHashMap<PseudoInstruction, GcbExpandPseudoInstructionCppFunction>)
            passResults.lastResultOf(
                PseudoExpansionFunctionGeneratorPass.class);
    var cppFunctions = cppFunctionsForPseudoInstructions.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    var pseudoInstructions =
        pseudoInstructions(specification, passResults, cppFunctions);
    var compilerInstructions = compilerInstructions(cppFunctions, specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().targetName().value().toLowerCase(), "pseudoInstructionsHeaders",
        Stream.concat(pseudoInstructions.stream(),
            compilerInstructions.stream()).map(e -> e.header).toList()
    );
  }
}
