package vadl.lcb.template.lib.Misc;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file registers benchmarks.
 */
public class EmitBenchmarkRegisterHeaderFilePass extends LcbTemplateRenderingPass {

  public EmitBenchmarkRegisterHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/utils/benchmark/src/benchmark_register.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/utils/benchmark/src/benchmark_register.h";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
