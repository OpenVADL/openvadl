package vadl.lcb.template.lib.Target;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
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

  static class LlvmRegisterFile extends RegisterFile {

    /**
     * Constructs a new RegisterFile object.
     **/
    public LlvmRegisterFile(RegisterFile registerFile) {
      super(registerFile.identifier, registerFile.addressType(), registerFile.resultType(),
          registerFile.constraints());
    }

    public String llvmResultType() {
      return ValueType.from(type()).get().getLlvmType();
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (DummyAbi) specification.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var registerFiles = ((GenerateRegisterClassesPass.Output) passResults.lastResultOf(
        GenerateRegisterClassesPass.class)).registerClasses();
    var framePointer = renderRegister(abi.framePointer().registerFile(), abi.framePointer().addr());
    var stackPointer = renderRegister(abi.stackPointer().registerFile(), abi.stackPointer().addr());
    var addressSequence = abi.callSequence();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
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
        ValueType.from(abi.stackPointer().registerFile().resultType()).get().getLlvmType(),
        "addressSequence", addressSequence);
  }
}
