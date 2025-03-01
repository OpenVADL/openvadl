package vadl.lcb.template.lib.Target.Utils;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.BaseInfoFunctionProvider;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file is a helper class.
 */
public class EmitBaseInfoFilePass extends LcbTemplateRenderingPass {

  public EmitBaseInfoFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Utils/BaseInfo.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/Utils/"
        + processorName + "BaseInfo.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var modifiers = output.modifiers();
    var linkModifierToRelocation = output
        .linkModifierToRelocation()
        .stream()
        .filter(distinctByKey(x -> x.left().value()))
        .map(x -> Map.of("modifier", x.left(),
            "relocation", x.right().valueRelocation().functionName().lower()))
        .toList();
    var relocations = BaseInfoFunctionProvider.getBaseInfoRecords(passResults);

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "isBigEndian", false,
        "relocations", relocations,
        "modifiers", modifiers,
        "linkModifierToRelocation", linkModifierToRelocation
    );
  }
}
