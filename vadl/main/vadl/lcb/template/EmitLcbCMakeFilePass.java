package vadl.lcb.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Register;
import vadl.viam.Specification;

/**
 * Makefile for the lcb.
 */
public class EmitLcbCMakeFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitLcbCMakeFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/LcbMakefile";
  }

  @Override
  protected String getOutputPath() {
    return "Makefile";
  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
