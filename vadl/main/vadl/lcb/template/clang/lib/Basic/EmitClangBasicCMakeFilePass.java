package vadl.lcb.template.clang.lib.Basic;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * CMake file for clang/lib/Basic.
 */
public class EmitClangBasicCMakeFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitClangBasicCMakeFilePass(LcbConfiguration lcbConfiguration,
                                     ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
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
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
