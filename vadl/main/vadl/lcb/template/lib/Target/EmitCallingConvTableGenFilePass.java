package vadl.lcb.template.lib.Target;

import static vadl.viam.ViamError.ensure;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;
import vadl.viam.Abi;
import vadl.viam.Specification;

/**
 * This file contains the calling conventions for the defined backend.
 */
public class EmitCallingConvTableGenFilePass extends LcbTemplateRenderingPass {
  public EmitCallingConvTableGenFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/CallingConv.td";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/" + processorName
        + "CallingConv.td";
  }

  record AssignToReg(String type, String registerRefs) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "type", type,
          "registerRefs", registerRefs
      );
    }
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var abi =
        (Abi) specification.definitions().filter(x -> x instanceof Abi).findFirst().get();
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "calleeRegisters", abi.calleeSaved().stream().map(Abi.RegisterRef::render).toList(),
        "functionRegisterType", getFuncArgsAssignToReg(abi).type,
        "functionRegisters", getFuncArgsAssignToReg(abi),
        "returnRegisters", getReturnAssignToReg(abi));
  }

  @Nonnull
  private AssignToReg getReturnAssignToReg(Abi abi) {
    ensure(abi.returnRegisters().stream().map(x -> x.registerFile().relationType()).collect(
            Collectors.toSet()).size() == 1,
        "All return registers must have the same type and at least one must exist");
    return new AssignToReg(
        ValueType.from(abi.returnRegisters().get(0).registerFile().resultType()).get()
            .getLlvmType(),
        abi.returnRegisters().stream().map(Abi.RegisterRef::render)
            .collect(Collectors.joining(", ")));
  }

  @Nonnull
  private AssignToReg getFuncArgsAssignToReg(Abi abi) {
    ensure(abi.argumentRegisters().stream().map(x -> x.registerFile().relationType()).collect(
            Collectors.toSet()).size() == 1,
        "All function argument registers must have the same type and at least one must exist");
    return new AssignToReg(
        ValueType.from(abi.argumentRegisters().get(0).registerFile().resultType()).get()
            .getLlvmType(),
        abi.argumentRegisters().stream().map(Abi.RegisterRef::render)
            .collect(Collectors.joining(", ")));
  }
}
