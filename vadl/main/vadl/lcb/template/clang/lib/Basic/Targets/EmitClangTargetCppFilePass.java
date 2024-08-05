package vadl.lcb.clang.lib.Basic.Targets;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Register;
import vadl.viam.Specification;

/**
 * This file contains the GCC reg names for clang.
 */
public class EmitClangTargetCppFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitClangTargetCppFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/clang/lib/Basic/Targets/ClangTarget.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "clang/lib/Basic/Targets/" + processorName.value() + ".cpp";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.REGISTERS, extractRegisters(specification));
  }

  private List<Register> extractRegisters(Specification specification) {
    return specification.isas()
        .flatMap(x -> x.registers().stream())
        .toList();
  }
}
