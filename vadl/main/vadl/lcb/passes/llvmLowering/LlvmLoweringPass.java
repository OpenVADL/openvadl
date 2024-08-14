package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.impl.LlvmLoweringArithmeticAndLogicStrategyImpl;
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
  private static final Logger logger = LoggerFactory.getLogger(
      LlvmLoweringPass.class);

  private final List<LlvmLoweringStrategy> strategies = List.of(
      new LlvmLoweringArithmeticAndLogicStrategyImpl()
  );

  public record LlvmLoweringIntermediateResult(Graph behavior,
                                               List<TableGenInstructionOperand> inputs,
                                               List<TableGenInstructionOperand> outputs,
                                               List<Graph> patterns) {

  }

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    Map<Instruction, LlvmLoweringIntermediateResult>
        llvmPatterns = new IdentityHashMap<>();
    var isaMatched =
        (HashMap<InstructionLabel, List<Instruction>>) passResults.get(
            new PassKey("IsaMatchingPass"));
    ensure(isaMatched != null, "Cannot find pass results from isaMatched");

    var instructionLookup = flipIsaMatching(isaMatched);

    viam.isas().flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> {
          for (var strategy : strategies) {
            if (!strategy.isApplicable(instructionLookup, instruction)) {
              continue;
            }

            var res = strategy.lower(instruction);

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
