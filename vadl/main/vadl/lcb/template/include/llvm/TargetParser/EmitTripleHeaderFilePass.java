package vadl.lcb.template.include.llvm.TargetParser;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains code which handles the triple.
 */
public class EmitTripleHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitTripleHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/TargetParser/Triple.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/include/llvm/TargetParser/Triple.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName());
  }
}
