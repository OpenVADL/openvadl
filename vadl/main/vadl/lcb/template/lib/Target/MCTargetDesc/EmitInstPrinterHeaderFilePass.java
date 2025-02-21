package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the definitions for emitting asm instructions.
 */
public class EmitInstPrinterHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitInstPrinterHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetInstPrinter.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "InstPrinter.h";
  }

  record RegisterClass(String simpleName) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var registerFiles =
        specification.registerFiles().map(x -> new RegisterClass(x.identifier.simpleName()))
            .toList();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        CommonVarNames.REGISTERS_CLASSES,
        registerFiles.stream().map(RegisterClass::simpleName).toList());
  }
}
