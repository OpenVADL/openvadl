package vadl.lcb.template.lib.Target.TargetInfo;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the definitions for the target definition.
 */
public class EmitTargetInfoHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitTargetInfoHeaderFilePass(LcbConfiguration lcbConfiguration,
                                      ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/TargetInfo/TargetInfo.h";
  }

  @Override
  protected String getOutputPath() {
    return "lcb/llvm/lib/Target/" + processorName.value() + "/TargetInfo/"
        + processorName.value() + "TargetInfo.h";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
