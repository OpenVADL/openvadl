package vadl.lcb.passes.llvmLowering.strategies.impl;

import static vadl.lcb.passes.isaMatching.InstructionLabel.ADD_32;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

public class LlvmLoweringConditionalsStrategyImpl implements LlvmLoweringStrategy {

  private final Set<InstructionLabel> supported = Set.of(InstructionLabel.LT);

  @Override
  public boolean isApplicable(Map<Instruction, InstructionLabel> matching,
                              Instruction instruction) {
    return false;
  }

  @Override
  public Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(
      Identifier instructionIdentifier, Graph behavior) {
    return Optional.empty();
  }
}
