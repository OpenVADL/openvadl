package vadl.lcb.template.clang.lib.CodeGen;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file handles various target-specific code generation issues.
 */
public class EmitCodeGenTargetInfoHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitCodeGenTargetInfoHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/CodeGen/TargetInfo.h";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/CodeGen/TargetInfo.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
