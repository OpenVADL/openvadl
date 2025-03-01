package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.ConcreteLogicalRelocation;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
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

  private List<ConcreteLogicalRelocation> formatModifier(PassResults passResults) {
    var container = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    return container.elfRelocations()
        .stream()
        .filter(x -> x instanceof ConcreteLogicalRelocation)
        .map(x -> (ConcreteLogicalRelocation) x)
        .filter(distinctByKey(LogicalRelocation::variantKind))
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
      var insns = isa.get().ownInstructions().stream().map(Definition::simpleName);
      var pseudoInsns = isa.get().ownPseudoInstructions().stream().map(Definition::simpleName);

      return Stream.concat(insns, pseudoInsns).toList();
    }
    return List.of();
  }

  private List<Map<String, String>> modifierMappings(Specification specification) {
    var mappings =
        specification.assemblyDescription().map(AssemblyDescription::modifiers).orElse(List.of());
    return mappings.stream().map(
        mapping -> Map.of(
            "modifier", mapping.simpleName(),
            "relocation", mapping.getRelocation().identifier.lower()
        )
    ).toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "registers",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .flatMap(x -> x.registers().stream()).toList(),
        "registerClasses",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .toList(),
        "formatModifiers", formatModifier(passResults),
        "asmCompareFunction", stringCompareFunction(specification),
        "instructionNames", instructionsNames(specification),
        "modifierMappings", modifierMappings(specification)
    );
  }
}
