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
 * CMake file for clang/lib/CodeGen/CMakeLists.txt.
 */
public class EmitCodeGenModuleCMakeFilePass extends LcbTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitCodeGenModuleCMakeFilePass(LcbConfiguration lcbConfiguration,
                                        ProcessorName processorName) throws IOException {
    super(lcbConfiguration);
    this.processorName = processorName;
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
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
