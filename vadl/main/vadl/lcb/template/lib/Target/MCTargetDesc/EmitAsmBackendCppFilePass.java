package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.Fixup;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This file contains the implementation for assembly fixups.
 */
public class EmitAsmBackendCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmBackendCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/AsmBackend.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/" + processorName
        + "AsmBackend.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var fixups = output.fixups();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "is64Bit", abi.stackPointer().registerFile().resultType().bitWidth() == 64,
        "fixups", fixups);
  }
}
