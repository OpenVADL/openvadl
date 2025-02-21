package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.List;
import java.util.stream.Collectors;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.viam.RegisterFile;

/**
 * Represents a single register file in TableGen. This is the lowered representation of a
 * {@link RegisterFile}.
 */
public record TableGenRegisterClass(ProcessorName namespace,
                                    String name,
                                    int alignment,
                                    List<ValueType> regTypes,
                                    List<TableGenRegister> registers,
                                    RegisterFile registerFileRef) {
  public String regTypesString() {
    return regTypes.stream().map(ValueType::getLlvmType).collect(Collectors.joining(", "));
  }
}
