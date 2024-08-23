package vadl.lcb.clang.lib.Basic.Targets;

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
 * This file contains the ABI information for chars.
 * See <a href="https://discourse.llvm.org/t/where-is-the-default-for-char-defined/79202/5">this</a> for more
 * information.
 */
public class EmitClangTargetsFilePass extends LcbTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitClangTargetsFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration);
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/Basic/Targets.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/Basic/Targets.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
