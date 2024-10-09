package vadl.lcb.passes.fixup.domain;

import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.viam.Relocation;

/**
 * Every {@link Fixup} is a {@link Relocation}. But not every {@link Relocation} is a {@link Fixup}.
 */
public class Fixup {
  private final LogicalRelocation logicalRelocation;

  public Fixup(LogicalRelocation relocation) {
    this.logicalRelocation = relocation;
  }

  public LogicalRelocation.Kind kind() {
    return logicalRelocation.kind();
  }

  public LogicalRelocation logicalRelocation() {
    return logicalRelocation;
  }

  public FixupName name() {
    return new FixupName(
        "fixup_" + logicalRelocation.relocation().identifier.lower());
  }
}
