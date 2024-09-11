package vadl.lcb.template.lib.Target.Disassembler;

import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.lcb.template.utils.ImmediateDecodingFunctionProvider;
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
    return specification.isa().map(x -> x.ownRegisterFiles().stream())
        .orElse(Stream.empty())
        .map(RegisterUtils::getRegisterClass)
        .toList();
  }

  private List<Immediate> extractImmediates(PassResults passResults) {
    return ImmediateDecodingFunctionProvider.generateDecodeFunctions(passResults)
        .entrySet()
        .stream()
        .map(entry -> {
          var field = entry.getKey();
          var simpleName = field.identifier.lower();
          var decoderMethod = entry.getValue().functionName().lower();
          var bitWidth = field.size();

          return new Immediate(simpleName, decoderMethod, bitWidth,
              (int) Math.pow(2, bitWidth) - 1);
        })
        .sorted(Comparator.comparing(o -> o.simpleName))
        .toList();
  }

  /**
   * Get the instruction bit size from the {@link Specification}.
   * The method throws an exception when different sizes exist.
   */
  private int getInstructionSize(Specification specification) {
    List<Integer> sizes = specification.isa()
        .map(x -> x.ownFormats().stream()).orElseGet(Stream::empty)
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
        extractImmediates(passResults),
        "instructionSize", getInstructionSize(specification),
        CommonVarNames.REGISTERS_CLASSES, extractRegisterClasses(specification));
  }
}
