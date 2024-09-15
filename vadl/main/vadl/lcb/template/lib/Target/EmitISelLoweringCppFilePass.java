package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.types.DataType;
import vadl.viam.Identifier;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * This file contains the legalization, promotions and legalization of nodes.
 */
public class EmitISelLoweringCppFilePass extends LcbTemplateRenderingPass {

  public EmitISelLoweringCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/ISelLowering.cpp";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "ISelLowering.cpp";
  }

  record LlvmRegisterClass(String namespace,
                           RegisterFile registerFile,
                           String name,
                           String regType) {

  }

  static class LlvmRegisterFile extends RegisterFile {

    /**
     * Constructs a new RegisterFile object.
     **/
    public LlvmRegisterFile(RegisterFile registerFile) {
      super(registerFile.identifier, registerFile.addressType(), registerFile.resultType(),
          registerFile.constraints());
    }

    public String llvmResultType() {
      return ValueType.from(type()).getLlvmType();
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var registerFiles = specification.registerFiles()
        .map(registerFile -> new LlvmRegisterClass(
            lcbConfiguration().processorName().value(),
            registerFile,
            registerFile.identifier.simpleName(),
            ValueType.from(registerFile.resultType()).getLlvmType()
        )).toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "registerFiles", registerFiles,
        "framePointer", framePointer,
        "stackPointer", stackPointer,
        "stackPointerByteSize", abi.stackPointer().registerFile().resultType().bitWidth() / 8,
        "argumentRegisterClasses", abi.argumentRegisters().stream().map(
                DummyAbi.RegisterRef::registerFile)
            .distinct()
            .map(LlvmRegisterFile::new)
            .toList(),
        "argumentRegisters", abi.argumentRegisters(),
        "stackPointerType",
        ValueType.from(abi.stackPointer().registerFile().resultType()).getLlvmType());
  }
}
