package vadl.lcb.passes.isaMatching.database;

import java.util.List;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * This is a container result structure for {@link Database}.
 */
public record QueryResult(List<Instruction> machineInstructions,
                          List<PseudoInstruction> pseudoInstructions) {
}
