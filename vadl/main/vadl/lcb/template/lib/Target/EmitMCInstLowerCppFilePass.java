package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the lowering logic to the MC layer.
 */
public class EmitMCInstLowerCppFilePass extends LcbTemplateRenderingPass {

  public EmitMCInstLowerCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCInstLower.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/"
        + processorName + "MCInstLower.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var linkModifiersToVariantKinds =
        ((GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class)).linkModifierToVariantKind();

    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "linkModifiersToVariantKinds", linkModifiersToVariantKinds.stream().map(
                pair -> Map.of("modifier", pair.left().value(),
                    "variantKind", pair.right().value()))
            .toList());
  }
}
