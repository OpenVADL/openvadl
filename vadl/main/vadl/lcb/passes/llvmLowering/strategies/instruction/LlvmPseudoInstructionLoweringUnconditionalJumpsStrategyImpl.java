package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.List;
import java.util.Set;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;

/**
 * Lowers unconditional jumps into TableGen.
 */
public class LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl extends
    LlvmPseudoInstructionLowerStrategy {
  /**
   * Constructor.
   */
  public LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl(
      List<LlvmInstructionLoweringStrategy> strategies) {
    super(strategies);
  }

  @Override
  protected Set<PseudoInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(PseudoInstructionLabel.J);
  }
}
