package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
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


  record RegisterClass(String simpleName) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "simpleName", simpleName
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var registerFiles =
        specification.registerFiles()
            .map(x -> new RegisterClass(x.identifier.simpleName()))
            .toList();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        CommonVarNames.REGISTERS_CLASSES, registerFiles,
        "registers",
        specification.registerFiles().map(x -> RegisterUtils.getRegisterClass(x, abi.aliases()))
            .flatMap(x -> x.registers().stream())
            .toList());
  }


}
