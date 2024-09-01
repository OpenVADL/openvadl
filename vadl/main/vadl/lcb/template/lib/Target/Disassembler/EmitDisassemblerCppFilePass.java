package vadl.lcb.template.lib.Target.Disassembler;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.encoding.DecodingCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.templateUtils.RegisterUtils;
import vadl.pass.PassResults;
import vadl.viam.Format;
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

  record Immediate(String simpleName, String decodeMethodName, int bitWidth, long mask) {

  }

  private List<RegisterUtils.RegisterClass> extractRegisterClasses(Specification specification) {
    return specification.isas()
        .flatMap(x -> x.ownRegisterFiles().stream())
        .map(RegisterUtils::getRegisterClass)
        .toList();
  }

  private List<Immediate> extractImmediates(Specification specification) {
    return specification.isas()
        .flatMap(x -> x.ownFormats().stream())
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .map(fieldAccess -> {
          var simpleName = fieldAccess.fieldRef().identifier.lower();
          var decodeMethodName = DecodingCodeGenerator.generateFunctionName(
              fieldAccess.accessFunction().identifier.lower());
          var bitWidth = fieldAccess.fieldRef().size();
          return new Immediate(simpleName, decodeMethodName, bitWidth,
              (int) Math.pow(2, bitWidth) - 1);
        })
        .toList();
  }

  /**
   * Get the instruction bit size from the {@link Specification}.
   * The method throws an exception when different sizes exist.
   */
  private int getInstructionSize(Specification specification) {
    List<Integer> sizes = specification.isas()
        .flatMap(x -> x.ownFormats().stream())
        .mapToInt(x -> Arrays.stream(x.fields()).mapToInt(Format.Field::size).sum())
        .distinct()
        .boxed()
        .toList();

    ensure(sizes.size() == 1, "Vadl only support a constant instruction size."
        + " Found multiple sizes");
    return sizes.get(0);
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "immediates",
        extractImmediates(specification),
        "instructionSize", getInstructionSize(specification),
        CommonVarNames.REGISTERS_CLASSES, extractRegisterClasses(specification));
  }
}
