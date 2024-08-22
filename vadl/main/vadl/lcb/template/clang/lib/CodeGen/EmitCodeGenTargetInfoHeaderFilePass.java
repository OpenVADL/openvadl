package vadl.lcb.template.clang.lib.CodeGen;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.pass.PassResults;
import vadl.pass.PassResults;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file handles various target-specific code generation issues.
 */
public class EmitCodeGenTargetInfoHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitCodeGenTargetInfoHeaderFilePass(LcbConfiguration lcbConfiguration,
                                             ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
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
