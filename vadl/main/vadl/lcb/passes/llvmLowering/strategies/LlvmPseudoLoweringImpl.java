package vadl.lcb.passes.llvmLowering.strategies;

import java.util.Optional;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Whereas {@link LlvmInstructionLoweringStrategy} defines multiple to lower {@link Instruction}
 * a.k.a Machine Instructions, this class lowers {@link PseudoInstruction}.
 */
public class LlvmPseudoLoweringImpl {

  public Optional<LlvmLoweringRecord> lower(PseudoInstruction pseudo) {
    return Optional.empty();
  }
}
