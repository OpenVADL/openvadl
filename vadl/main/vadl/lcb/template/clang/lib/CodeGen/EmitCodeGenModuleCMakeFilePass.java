package vadl.lcb.template.clang.lib.CodeGen;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * CMake file for clang/lib/CodeGen/CMakeLists.txt.
 */
public class EmitCodeGenModuleCMakeFilePass extends LcbTemplateRenderingPass {

  public EmitCodeGenModuleCMakeFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/CodeGen/CMakeLists.txt";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/CodeGen/CMakeLists.txt";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName());
  }
}
