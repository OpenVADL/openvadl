package vadl.lcb.passes.isaMatching.database;

import javax.annotation.Nullable;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;

public class Query {
  @Nullable
  private MachineInstructionLabel machineInstructionLabel;

  @Nullable
  private PseudoInstructionLabel pseudoInstructionLabel;

  public Query(@Nullable MachineInstructionLabel machineInstructionLabel,
               @Nullable PseudoInstructionLabel pseudoInstructionLabel) {
    this.machineInstructionLabel = machineInstructionLabel;
    this.pseudoInstructionLabel = pseudoInstructionLabel;
  }

  @Nullable
  public MachineInstructionLabel machineInstructionLabel() {
    return machineInstructionLabel;
  }

  @Nullable
  public PseudoInstructionLabel pseudoInstructionLabel() {
    return pseudoInstructionLabel;
  }

  public static class Builder {

    @Nullable
    private MachineInstructionLabel machineInstructionLabel;

    @Nullable
    private PseudoInstructionLabel pseudoInstructionLabel;

    public Builder machineInstructionLabel(MachineInstructionLabel machineInstructionLabel) {
      this.machineInstructionLabel = machineInstructionLabel;
      return this;
    }

    public Builder pseudoInstructionLabel(PseudoInstructionLabel pseudoInstructionLabel) {
      this.pseudoInstructionLabel = pseudoInstructionLabel;
      return this;
    }
  }
}
