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

import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.gcb.passes.relocation.model.ImplementedUserSpecifiedRelocation;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.AssemblyDescription;
import vadl.viam.Definition;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmParserCaseSensitive;

/**
 * This file includes the util functions for asm.
 */
public class EmitAsmUtilsCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmUtilsCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/AsmUtils.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/AsmUtils.cpp";
  }

  private List<ModifierAggregate> formatModifier(PassResults passResults) {
    var container = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    return container.elfRelocations()
        .stream()
        .filter(x -> x instanceof ImplementedUserSpecifiedRelocation)
        .map(x -> (ImplementedUserSpecifiedRelocation) x)
        .filter(distinctByKey(ImplementedUserSpecifiedRelocation::variantKind))
        .map(x -> new ModifierAggregate(x.variantKind().value(), x.relocation().simpleName()))
        .toList();
  }

  private String stringCompareFunction(Specification specification) {
    var isCaseSensitive = specification.assemblyDescription()
        .map(asmDesc -> asmDesc.annotation(AsmParserCaseSensitive.class))
        .map(AsmParserCaseSensitive::isCaseSensitive).orElse(false);

    return isCaseSensitive ? "equals" : "equals_insensitive";
  }

  private List<String> instructionsNames(Specification specification) {
    var isa = specification.isa();
    if (isa.isPresent()) {
      var instructions = isa.get().ownInstructions().stream().map(Definition::simpleName);
      var pseudoInstructions =
          isa.get().ownPseudoInstructions().stream().map(Definition::simpleName);

      return Stream.concat(instructions, pseudoInstructions).toList();
    }
    return List.of();
  }

  private List<Map<String, String>> modifierMappings(
      Specification specification,
      GenerateLinkerComponentsPass.Output linkerInformation) {
    var mappings =
        specification.assemblyDescription()
            .map(AssemblyDescription::modifiers)
            .orElse(Collections.emptyList());

    return mappings.stream().map(
        mapping -> {
          var elfRelocation =
              ensurePresent(
                  linkerInformation.elfRelocations().stream()
                      .filter(x -> x.relocation().equals(mapping.getRelocation()))
                      .findFirst(), () -> Diagnostic.error(
                      "Cannot find an ELF relocation for the given relocation function.",
                      mapping.getRelocation().sourceLocation()));

          var variantKind = elfRelocation.variantKind();
          return Map.of(
              "modifier", mapping.simpleName(),
              "variantKind", variantKind.value()
          );
        }
    ).toList();
  }

  record ModifierAggregate(String variantKind, String relocationName) implements Renderable {
    @Override
    public Map<String, Object> renderObj() {
      return Map.of("variantKind", variantKind,
          "relocationName", relocationName);
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var linkerInformation = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var modifiers = formatModifier(passResults);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "registers",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .flatMap(x -> x.registers().stream()).toList(),
        "registerClasses",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .toList(),
        "asmCompareFunction", stringCompareFunction(specification),
        "instructionNames", instructionsNames(specification),
        "modifierMappings", modifierMappings(specification, linkerInformation),
        "formatModifiers", modifiers
    );
  }
}
