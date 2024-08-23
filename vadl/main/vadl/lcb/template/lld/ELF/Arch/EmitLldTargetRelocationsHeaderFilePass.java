package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file defines the relocations for the linker.
 */
public class EmitLldTargetRelocationsHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitLldTargetRelocationsHeaderFilePass(LcbConfiguration lcbConfiguration,
                                                ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/TargetRelocations.hpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + processorName.value() + "Relocations.hpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
