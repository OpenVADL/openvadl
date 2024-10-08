package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateVariantKindProvider;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the definitions for emitting MC operands.
 */
public class EmitMCExprHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitMCExprHeaderFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetMCExpr.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/MCTargetDesc/"
        + processorName + "MCExpr.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "relocations", relocations(passResults),
        "immediates", ImmediateVariantKindProvider.variantKinds(passResults));
  }

  private List<ElfRelocation> relocations(PassResults passResults) {
    return (List<ElfRelocation>) passResults.lastResultOf(GenerateElfRelocationPass.class);
  }
}
