package vadl.lcb.include.llvm.Object;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file declares the ELFObjectFile template class.
 */
public class EmitELFObjectHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitELFObjectHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
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
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase());
  }
}
