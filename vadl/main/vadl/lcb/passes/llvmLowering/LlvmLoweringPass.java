package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.impl.LlvmLoweringArithmeticAndLogicStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.impl.LlvmLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.impl.LlvmLoweringConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.impl.LlvmLoweringIndirectJumpStrategyImpl;
import vadl.lcb.passes.llvmLowering.visitors.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * This is a wrapper class which contains utility functions for the lowering.
 */
public class LlvmLoweringPass extends Pass {

  private final List<LlvmLoweringStrategy> strategies = List.of(
      new LlvmLoweringArithmeticAndLogicStrategyImpl(),
      new LlvmLoweringConditionalsStrategyImpl(),
      new LlvmLoweringConditionalBranchesStrategyImpl(),
      new LlvmLoweringIndirectJumpStrategyImpl()
  );

  /**
   * Contains information for the lowering of instructions.
   *
   * @param behavior has replaced nodes from {@link ReplaceWithLlvmSDNodesVisitor}.
   * @param inputs   are the input operands for the tablegen instruction.
   * @param outputs  are the output operands for the tablegen instruction.
   * @param patterns are a list of {@link Graph} which contain the pattern selectors for the
   *                 tablegen instruction.
   */
  public record LlvmLoweringIntermediateResult(Graph behavior,
                                               List<TableGenInstructionOperand> inputs,
                                               List<TableGenInstructionOperand> outputs,
                                               List<LlvmLoweringTableGenPattern> patterns) {

  }

  /**
   * TableGen pattern has a tree for LLVM Dag nodes to select a pattern in the instruction
   * selection. This is represented by {@code selector}.
   * And a tree for the emitted machine instruction. This is represented by {@code machine}.
   */
  public record LlvmLoweringTableGenPattern(Graph selector, Graph machine) {
  }

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    IdentityHashMap<Instruction, Graph> uninlined =
        (IdentityHashMap<Instruction, Graph>) passResults.get(new PassKey("FunctionInlinerPass"));
    ensureNonNull(uninlined, "Inlined Function data must exist");
    IdentityHashMap<Instruction, LlvmLoweringIntermediateResult>
        llvmPatterns = new IdentityHashMap<>();
    var supportedInstructions =
        (HashMap<InstructionLabel, List<Instruction>>) passResults.get(
            new PassKey("IsaMatchingPass"));
    ensure(supportedInstructions != null, "Cannot find pass results from IsaMatchPass");

    var instructionLookup = flipIsaMatching(supportedInstructions);

    viam.isas().flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> {
          var instructionLabel = instructionLookup.get(instruction);

          if (instructionLabel == null) {
            return;
          }

          var uninlinedBehavior = uninlined.getOrDefault(instruction, instruction.behavior());
          for (var strategy : strategies) {
            if (!strategy.isApplicable(instructionLabel)) {
              continue;
            }

            var res =
                strategy.lower(supportedInstructions, instruction, instructionLabel,
                    uninlinedBehavior);

            res.ifPresent(llvmLoweringIntermediateResult -> llvmPatterns.put(instruction,
                llvmLoweringIntermediateResult));
          }
        });

    return llvmPatterns;
  }

  /**
   * The {@link IsaMatchingPass} computes a hashmap with the instruction label as a key and all
   * the matched instructions as value.
   * However, we would like to check whether {@link LlvmLoweringStrategy} supports this
   * {@link Instruction} in this pass. That's why we have the flip the hashmap.
   */
  private IdentityHashMap<Instruction, InstructionLabel> flipIsaMatching(
      HashMap<InstructionLabel, List<Instruction>> isaMatched) {
    IdentityHashMap<Instruction, InstructionLabel> inverse = new IdentityHashMap<>();

    for (var entry : isaMatched.entrySet()) {
      for (var item : entry.getValue()) {
        inverse.put(item, entry.getKey());
      }
    }

    return inverse;
  }


}
