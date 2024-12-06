package vadl.lcb.passes.isaMatching.database;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.util.List;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * This is a container result structure for {@link Database}.
 */
public record QueryResult(Query executedQuery,
                          List<Instruction> machineInstructions,
                          List<PseudoInstruction> pseudoInstructions) {
  public Instruction firstMachineInstruction() {
    ensure(pseudoInstructions.isEmpty(),
        "Cannot get first machine instruction when there are pseudo instructions");
    return ensurePresent(machineInstructions.stream().findFirst(),
        "There has to be at least one machine instruction");
  }

  public PseudoInstruction firstPseudoInstruction() {
    ensure(machineInstructions.isEmpty(),
        "Cannot get first pseudo instruction when there are machine instructions");
    return ensurePresent(pseudoInstructions.stream().findFirst(),
        "There has to be at least one pseudo instruction");
  }
}
