package vadl.lcb.include.llvm.Object;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file declares the ELFObjectFile template class.
 */
public class EmitELFObjectHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitELFObjectHeaderFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/Object/ELFObjectFile.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/include/llvm/Object/ELFObjectFile.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
