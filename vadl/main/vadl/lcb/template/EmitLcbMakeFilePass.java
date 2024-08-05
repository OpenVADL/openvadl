package vadl.lcb.template;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * Makefile for the lcb.
 */
public class EmitLcbMakeFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitLcbMakeFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/LcbMakeFile";
  }

  @Override
  protected String getOutputPath() {
    return "Makefile";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
