package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Whereas {@link LlvmInstructionLoweringStrategy} defines multiple to lower {@link Instruction}
 * a.k.a Machine Instructions, this class lowers {@link PseudoInstruction}. But only as a fallback
 * strategy when no other strategy is applicable.
 */
public class LlvmPseudoInstructionLoweringDefaultStrategyImpl
    extends LlvmPseudoInstructionLowerStrategy {
  public LlvmPseudoInstructionLoweringDefaultStrategyImpl(
      List<LlvmInstructionLoweringStrategy> strategies) {
    super(strategies);
  }

  @Override
  protected Set<PseudoInstructionLabel> getSupportedInstructionLabels() {
    return Set.of();
  }

  @Override
  public boolean isApplicable(@Nullable PseudoInstructionLabel pseudoInstructionLabel) {
    // This is strategy should be always applicable.
    return true;
  }
}
