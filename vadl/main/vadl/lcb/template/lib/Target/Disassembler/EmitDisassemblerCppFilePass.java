package vadl.lcb.template.lib.Target.Disassembler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

/**
 * This file contains the target specific implementation for the disassembler.
 */
public class EmitDisassemblerCppFilePass extends LcbTemplateRenderingPass {

  public EmitDisassemblerCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Disassembler/TargetDisassembler.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().processorName().value() + "/Disassembler/"
        + lcbConfiguration().processorName().value()
        + "Disassembler.cpp";
  }

  record LoweredImmediate(String identifier, Decode decoding) {

  }

  record Immediate(LoweredImmediate loweredImmediate, int size) {

  }

  record Decode(String identifier) {

  }

  private List<RegisterFile> extractRegisterClasses(Specification specification) {
    return specification.isas()
        .flatMap(x -> x.registerFiles().stream())
        .toList();
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "immediates",
        List.of(new Immediate(
            new LoweredImmediate("immediateValue",
                new Decode("decodingValue")),
            8)),
        "instructionSize", 32, // bits
        CommonVarNames.REGISTERS_CLASSES, extractRegisterClasses(specification));
  }
}
