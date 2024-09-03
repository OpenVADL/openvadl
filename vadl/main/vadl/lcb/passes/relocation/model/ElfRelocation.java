package vadl.lcb.passes.relocation.model;

import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.viam.Relocation;

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

  public Relocation function() {
    return logicalRelocation.relocation();
  }

  public ElfRelocationName name() {
    return new ElfRelocationName(
        "R_" + processorName.value() + "_" + logicalRelocation.name().value());
  }
}
