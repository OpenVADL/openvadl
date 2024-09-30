package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file includes the definitions for util functions for asm.
 */
public class EmitAsmUtilsHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitAsmUtilsHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/AsmUtils.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().processorName().value()
        + "/MCTargetDesc/AsmUtils.h";
  }


  record RegisterClass(String simpleName) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var registerFiles =
        specification.registerFiles()
            .map(x -> new RegisterClass(x.identifier.simpleName()))
            .toList();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        CommonVarNames.REGISTERS_CLASSES, registerFiles);
  }
}
