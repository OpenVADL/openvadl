package vadl.lcb.template.lib.Target.Utils;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.RelocationLowerable;
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
    var elfRelocations = output.elfRelocations();
    var relocations = BaseInfoFunctionProvider.getBaseInfoRecords(passResults);

    var mos = elfRelocations.stream()
        .filter(x -> x instanceof RelocationLowerable)
        .map(x -> (RelocationLowerable) x)
        .filter(distinctByKey(x -> x.valueRelocation().functionName().lower()))
        .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "isBigEndian", false,
        "relocations", relocations,
        "mos", mos
    );
  }
}
