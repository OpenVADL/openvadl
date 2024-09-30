package vadl.lcb.clang.lib.Basic.Targets;

import static vadl.lcb.template.utils.DataLayoutProvider.createDataLayout;
import static vadl.lcb.template.utils.DataLayoutProvider.createDataLayoutString;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains the datatype configuration for the llvm's types.
 */
public class EmitClangTargetHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitClangTargetHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/Basic/Targets/ClangTarget.h";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/Basic/Targets/" + lcbConfiguration().processorName().value() + ".h";
  }


  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var gpr = ensurePresent(specification.registerFiles().findFirst(),
        "Specification requires at least one register file");
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.DATALAYOUT, createDataLayoutString(createDataLayout(gpr)));
  }
}
