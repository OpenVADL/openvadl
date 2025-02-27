package vadl.lcb.passes.isaMatching;

import java.util.Set;
import vadl.viam.ViamError;

/**
 * The {@link MachineInstructionLabelGroup} groups together multiple {@link MachineInstructionLabel}
 * which makes it more convenient to query for it in
 * {@link vadl.lcb.passes.isaMatching.database.Database}.
 */
public enum MachineInstructionLabelGroup {
  BRANCH_INSTRUCTIONS,
  MEMORY_INSTRUCTIONS,
  CONDITIONAL_INSTRUCTIONS,
  /* but only those which need to be checked in InstrInfo.cpp */
  AS_CHEAP_AS_MOVE_CANDIDATES;

  public static final Set<MachineInstructionLabel> branchMachineInstructions = Set.of(
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

  public static final Set<MachineInstructionLabel> conditionalInstructions = Set.of(
      MachineInstructionLabel.EQ,
      MachineInstructionLabel.NEQ,
      MachineInstructionLabel.LTI,
      MachineInstructionLabel.LTIU,
      MachineInstructionLabel.LTS,
      MachineInstructionLabel.LTU
  );

  public static final Set<MachineInstructionLabel> memoryMachineInstructions = Set.of(
      MachineInstructionLabel.LOAD_MEM,
      MachineInstructionLabel.STORE_MEM
  );

  public static final Set<MachineInstructionLabel> asCheapAsMoveCandidates = Set.of(
      MachineInstructionLabel.XORI,
      MachineInstructionLabel.ADDI_32,
      MachineInstructionLabel.ADDI_64,
      MachineInstructionLabel.ORI
  );

  /**
   * Return the set of instructions based on the value in the enum.
   */
  public final Set<MachineInstructionLabel> labels() {
    if (this == BRANCH_INSTRUCTIONS) {
      return branchMachineInstructions;
    } else if (this == MEMORY_INSTRUCTIONS) {
      return memoryMachineInstructions;
    } else if (this == CONDITIONAL_INSTRUCTIONS) {
      return conditionalInstructions;
    } else if (this == AS_CHEAP_AS_MOVE_CANDIDATES) {
      return asCheapAsMoveCandidates;
    }

    throw new ViamError("not supported");
  }
}
