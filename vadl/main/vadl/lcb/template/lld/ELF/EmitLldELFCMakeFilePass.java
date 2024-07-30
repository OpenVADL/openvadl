package vadl.lcb.lld.ELF;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * CMakefile for lld.
 */
public class EmitLldELFCMakeFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitLldELFCMakeFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/CMakeLists.txt";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/CMakeLists.txt";
  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
