package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.impl.LlvmLoweringConditionalBranchesStrategyImpl;
import vadl.viam.Instruction;

/**
 * Different {@link LlvmLoweringStrategy} can lower instructions differently.
 * Usually, we will reject any control-flow like if-conditions.
 * However, {@link LlvmLoweringConditionalBranchesStrategyImpl} uses information from the
 * {@link IsaMatchingPass} so it is lowerable for these {@link Instruction}.
 */
public interface TableGenPatternLowerable {

  /**
   * Returns whether the visitor encountered nodes which are not
   * lowerable.
   *
   * @return {@code true} when the graph is ok.
   */
  boolean isPatternLowerable();
}
