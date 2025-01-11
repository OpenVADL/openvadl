package vadl.lcb.passes.isaMatching;

import java.util.Set;
import vadl.viam.ViamError;

/**
 * The {@link MachineInstructionLabelGroup} groups together multiple {@link MachineInstructionLabel}
 * which makes it more convenient to query for it in
 * {@link vadl.lcb.passes.isaMatching.database.Database}.
 */
public enum MachineInstructionLabelGroup {
  BRANCH_INSTRUCTIONS;

  public final static Set<MachineInstructionLabel> branchMachineInstructions = Set.of(
      MachineInstructionLabel.BEQ,
      MachineInstructionLabel.BSGEQ,
      MachineInstructionLabel.BSGTH,
      MachineInstructionLabel.BSLEQ,
      MachineInstructionLabel.BSLTH,
      MachineInstructionLabel.BUGEQ,
      MachineInstructionLabel.BUGTH,
      MachineInstructionLabel.BULEQ,
      MachineInstructionLabel.BULTH,
      MachineInstructionLabel.BNEQ
  );

  /**
   * Return the set of instructions based on the value in the enum.
   */
  public final Set<MachineInstructionLabel> labels() {
    if (this == BRANCH_INSTRUCTIONS) {
      return branchMachineInstructions;
    }

    throw new ViamError("not supported");
  }
}
