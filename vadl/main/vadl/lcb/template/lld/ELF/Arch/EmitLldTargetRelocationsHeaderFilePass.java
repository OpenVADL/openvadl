package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.gcb.passes.relocation.model.RelocationLowerable;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file defines the relocations for the linker.
 */
public class EmitLldTargetRelocationsHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitLldTargetRelocationsHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/TargetRelocations.hpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + lcbConfiguration().processorName().value() + "Relocations.hpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var relocations = output.elfRelocations();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "relocations", relocations.stream()
            .filter(x -> x instanceof RelocationLowerable)
            .map(x -> (RelocationLowerable) x)
            .sorted(Comparator.comparing(o -> o.elfRelocationName().value()))
            .map(relocation -> {
              var generator = new ValueRelocationFunctionCodeGenerator(relocation.valueRelocation());
              return generator.genReturnExpression();
            }).toList());
  }
}
