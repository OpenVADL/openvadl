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

/**
 * This file defines the fixups.
 */
public class EmitFixupKindsHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitFixupKindsHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetFixupKinds.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "FixupKinds.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output = (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
        GenerateLinkerComponentsPass.class);
    var fixups = output.fixups();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "fixups", fixups);
  }
}
