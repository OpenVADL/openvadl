package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.relocation.RelocationOverrideCodeGenerator;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.lcb.passes.relocation.model.ElfRelocation;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the function to update an immediate when a relocation has to be applied.
 */
public class EmitLldManualEncodingHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitLldManualEncodingHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/TargetManualEncoding.hpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + lcbConfiguration().processorName().value() + "ManualEncoding.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var elfRelocations =
        (List<ElfRelocation>) passResults.lastResultOf(GenerateElfRelocationPass.class);
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "functions", elfRelocations.stream()
            .map(elfRelocation -> new RelocationOverrideCodeGenerator().generateFunction(
                elfRelocation.updateFunction())).toList());
  }
}
