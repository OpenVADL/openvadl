package vadl.lcb.clang.lib.Basic.Targets;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Register;
import vadl.viam.Specification;

/**
 * This file contains the GCC reg names for clang.
 */
public class EmitClangTargetCppFilePass extends AbstractTemplateRenderingPass {

  record Register(String name, List<String> aliases) {

  }

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
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.REGISTERS, extractRegisters(specification));
  }


  private List<Register> extractRegisters(Specification specification) {
    return specification.definitions()
        .filter(x -> x instanceof vadl.viam.Register)
        .map(x -> (vadl.viam.Register) x)
        .map(x -> new Register(x.name(), List.of("aliasValue1", "aliasValue2")))
        .toList();
  }
}
