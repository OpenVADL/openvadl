package vadl.lcb.clang.lib.Basic.Targets;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Register;
import vadl.viam.Specification;

/**
 * This file contains the GCC reg names for clang.
 */
public class EmitClangTargetCppFilePass extends LcbTemplateRenderingPass {

  public EmitClangTargetCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/Basic/Targets/ClangTarget.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/Basic/Targets/" + lcbConfiguration().processorName().value() + ".cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        CommonVarNames.REGISTERS, extractRegisters(specification));
  }

  private List<Register> extractRegisters(Specification specification) {
    return specification.isa()
        .map(x -> x.ownRegisters().stream())
        .orElseGet(Stream::empty)
        .toList();
  }
}
