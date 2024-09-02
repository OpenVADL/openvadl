package vadl.lcb.passes.relocation.model;

import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.gcb.valuetypes.ProcessorName;

public class ElfRelocation {
  private final ProcessorName processorName;
  private final LogicalRelocation relocation;

  public ElfRelocation(ProcessorName processorName, LogicalRelocation relocation) {
    this.processorName = processorName;
    this.relocation = relocation;
  }

  public LogicalRelocation.Kind kind() {
    return relocation.kind();
  }

  public ElfRelocationName name() {
    return new ElfRelocationName("R_" + processorName.value() + "_" + relocation.name().value());
  }
}
