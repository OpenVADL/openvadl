package vadl.lcb.template.clang.lib.Basic;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * CMake file for clang/lib/Basic.
 */
public class EmitClangBasicCMakeFilePass extends LcbTemplateRenderingPass {

  public EmitClangBasicCMakeFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/Basic/CMakeLists.txt";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/Basic/CMakeLists.txt";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
