package vadl.lcb.passes.isaMatching;

import java.util.Set;

/**
 * The {@link MachineInstructionLabelGroup} groups together multiple {@link MachineInstructionLabel}
 * which makes it more convenient to query for it in
 * {@link vadl.lcb.passes.isaMatching.database.Database}.
 */
public class MachineInstructionLabelGroup {
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
}
