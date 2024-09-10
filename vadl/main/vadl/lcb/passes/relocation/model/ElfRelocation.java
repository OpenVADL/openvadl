package vadl.lcb.passes.relocation.model;

import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.viam.Function;
import vadl.viam.Relocation;

/**
 * Wrapper for {@link LogicalRelocation}.
 */
public class ElfRelocation {
  private final ProcessorName processorName;
  private final LogicalRelocation logicalRelocation;

  public ElfRelocation(ProcessorName processorName, LogicalRelocation relocation) {
    this.processorName = processorName;
    this.logicalRelocation = relocation;
  }

  public LogicalRelocation.Kind kind() {
    return logicalRelocation.kind();
  }

  public LogicalRelocation logicalRelocation() {
    return logicalRelocation;
  }

  public CppFunction updateFunction() {
    return logicalRelocation.updateFunction();
  }

  public ElfRelocationName name() {
    return new ElfRelocationName(
        "R_" + processorName.value() + "_" + logicalRelocation.name().value());
  }
}
