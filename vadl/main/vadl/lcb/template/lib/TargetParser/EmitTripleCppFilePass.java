package vadl.lcb.lib.TargetParser;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains code which handles the triple.
 */
public class EmitTripleCppFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitTripleCppFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/TargetParser/Triple.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + processorName.value() + "/TargetParser/Triple.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
