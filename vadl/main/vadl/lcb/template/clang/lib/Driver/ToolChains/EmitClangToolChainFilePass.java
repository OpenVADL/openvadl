package vadl.lcb.clang.lib.Driver.ToolChains;

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
public class EmitClangToolChainFilePass extends LcbTemplateRenderingPass {
  public EmitClangToolChainFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/CodeGen/Driver/Toolchains/Clang.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/CodeGen/Driver/ToolChains/Clang.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
