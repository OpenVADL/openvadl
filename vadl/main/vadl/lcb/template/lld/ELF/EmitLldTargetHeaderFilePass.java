package vadl.lcb.lld.ELF;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This files contains the definitions for Lld's target.
 */
public class EmitLldTargetHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitLldTargetHeaderFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Target.h";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Target.h";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
