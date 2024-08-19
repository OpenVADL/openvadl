package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file is a helper file which includes all the lowered immediate files.
 */
public class EmitImmediateUtilsHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitImmediateUtilsHeaderFilePass(LcbConfiguration lcbConfiguration,
                                          ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/ImmediateUtils.h";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/ImmediateUtils.h";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
