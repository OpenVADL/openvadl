package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the CMakefile for the MCTarget.
 */
public class EmitMCTargetDescCMakeFilePass extends LcbTemplateRenderingPass {

  public EmitMCTargetDescCMakeFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/CMakeLists.txt";
  }

  @Override
  protected String getOutputPath() {
    return "lcb/llvm/lib/Target/" + lcbConfiguration().processorName().value()
        + "/MCTargetDesc/CMakeLists.txt";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
