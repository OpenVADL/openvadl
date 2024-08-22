package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the lowering logic to the MC layer.
 */
public class EmitMCInstLowerCppFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitMCInstLowerCppFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCInstLower.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "lcb/llvm/lib/Target/" + processorName.value() + "/"
        + processorName.value() + "MCInstLower.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
