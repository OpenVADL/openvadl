package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This file contains the register definitions for compiler backend.
 */
public class EmitRegisterInfoTableGenFilePass extends LcbTemplateRenderingPass {

  public EmitRegisterInfoTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/RegisterInfo.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "RegisterInfo.td";
  }

  record LlvmRegister(String namespace,
                      String id,
                      String asmName,
                      String altNames,
                      String aliases,
                      String subRegs,
                      String subRegIndices,
                      int coveredBySubRegs,
                      int hwEncodingMsb,
                      int hwEncodingValue) {

  }

  record LlvmRegisterClass(String namespace,
                           String name,
                           String regType,
                           // Alignment - Specify the alignment required of the registers when
                           // they are stored or loaded to memory.
                           int alignment,
                           // The order of the regList matters
                           // and indicates how the register allocator
                           // chooses the registers.
                           String regList) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var registerFiles = specification.registerFiles()
        .map(registerFile -> new LlvmRegisterClass(lcbConfiguration().processorName().value(),
            registerFile.identifier.simpleName(),
            ValueType.from(registerFile.resultType()).getLlvmType(),
            32, //TODO make changeable in spec
            getRegisterClass(registerFile, abi)
        )).toList();

    var isaRegisters = specification
        .registers()
        .map(x -> new LlvmRegister(
            lcbConfiguration().processorName().value(),
            x.identifier.simpleName(),
            "",
            "",
            "",
            "",
            "",
            0,
            0,
            0
        ))
        .toList();

    var registers =
        specification.registerFiles()
            .map(registerFile -> getRegisters(registerFile, abi))
            .flatMap(Collection::stream)
            .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "registerFiles", registerFiles,
        "registers", Stream.concat(isaRegisters.stream(), registers.stream()).toList());
  }

  private List<LlvmRegister> getRegisters(RegisterFile registerFile, DummyAbi abi) {
    var bitWidth = registerFile.addressType().bitWidth();
    var numberOfRegisters = (int) Math.pow(2, bitWidth);

    return IntStream.range(0, numberOfRegisters)
        .mapToObj(number -> {
          var alias = Optional.ofNullable(abi.aliases().get(Pair.of(registerFile, number)));
          var altNames =
              alias.map(registerAlias -> String.join(", ", wrapInQuotes(registerAlias.value()),
                      wrapInQuotes(registerFile.identifier.simpleName() + number)))
                  .orElseGet(() -> wrapInQuotes(registerFile.identifier.simpleName() + number));
          return new LlvmRegister(lcbConfiguration().processorName().value(),
              registerFile.identifier.simpleName() + number,
              alias.orElse(new DummyAbi.RegisterAlias(registerFile.identifier.simpleName()))
                  .value(),
              altNames,
              "",
              "",
              "",
              0,
              bitWidth - 1,
              number
          );
        })
        .toList();
  }

  private String wrapInQuotes(String value) {
    return "\"" + value + "\"";
  }

  private String getRegisterClass(RegisterFile registerFile, DummyAbi abi) {
    var bitWidth = registerFile.addressType().bitWidth();
    var numberOfRegisters = (int) Math.pow(2, bitWidth);
    var allRegisters = IntStream.range(0, numberOfRegisters)
        .mapToObj(x -> x + "")
        .collect(Collectors.toList());
    var callerSaved = abi.callerSaved().stream().map(x -> x.addr() + "")
        .toList();
    var calleeSaved = abi.calleeSaved().stream().map(x -> x.addr() + "")
        .toList();
    allRegisters.removeAll(callerSaved);
    allRegisters.removeAll(calleeSaved);

    return Stream.concat(callerSaved.stream(),
            Stream.concat(
                calleeSaved.stream(),
                allRegisters.stream()))
        .collect(Collectors.joining(", "));
  }


}
