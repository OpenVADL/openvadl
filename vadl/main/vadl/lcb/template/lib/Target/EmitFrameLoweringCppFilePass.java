package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Abi;
import vadl.viam.Specification;

/**
 * This file contains the logic for lowering stack frames.
 */
public class EmitFrameLoweringCppFilePass extends LcbTemplateRenderingPass {

  public EmitFrameLoweringCppFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/FrameLowering.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "FrameLowering.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var returnAddress =
        renderRegister(abi.returnAddress().registerFile(), abi.returnAddress().addr());
    var stackAlignment = abi.stackAlignment();
    var transientStackAlignment = abi.transientStackAlignment();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "hasFramePointer", abi.hasFramePointer(),
        "framePointer", framePointer,
        "stackPointer", stackPointer,
        "returnAddress", returnAddress,
        "stackAlignment", stackAlignment.inBytes(),
        "transientStackAlignment", transientStackAlignment.inBytes());
  }
}
