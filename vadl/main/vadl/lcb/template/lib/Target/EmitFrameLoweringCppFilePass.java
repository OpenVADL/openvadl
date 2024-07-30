package vadl.lcb.lib.Target;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the logic for lowering stack frames.
 */
public class EmitFrameLoweringCppFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitFrameLoweringCppFilePass(LcbConfiguration lcbConfiguration,
                                      ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/Target/FrameLowering.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/Target/" + processorName.value() + "/" + processorName.value()
        + "FrameLowering.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
