package vadl.lcb.template.lib.TargetParser;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.Abi;

/**
 * This file contains code which handles the triple.
 */
public class EmitTripleCppFilePass extends LcbTemplateRenderingPass {

  public EmitTripleCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/TargetParser/Triple.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/TargetParser/Triple.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "pointerBitWidth", abi.stackPointer().registerFile().addressType().bitWidth(),
        "isLittleEndian", true); // TODO kper make adjustable
  }
}
