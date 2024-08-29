package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringAddImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringArithmeticAndLogicStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringIndirectJumpStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryLoadStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryStoreStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.visitors.impl.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Register;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * This is a wrapper class which contains utility functions for the lowering.
 */
public class LlvmLoweringPass extends Pass {

  private final List<LlvmInstructionLoweringStrategy> strategies = List.of(
      new LlvmInstructionLoweringArithmeticAndLogicStrategyImpl(),
      new LlvmInstructionLoweringAddImmediateStrategyImpl(),
      new LlvmInstructionLoweringConditionalsStrategyImpl(),
      new LlvmInstructionLoweringConditionalBranchesStrategyImpl(),
      new LlvmInstructionLoweringIndirectJumpStrategyImpl(),
      new LlvmInstructionLoweringMemoryStoreStrategyImpl(),
      new LlvmInstructionLoweringMemoryLoadStrategyImpl()
  );

  public LlvmLoweringPass(LcbConfiguration configuration) {
    super(configuration);
  }

  /**
   * A {@link TableGenInstruction} has many boolean flags which are required for the
   * code generation.
   */
  public record Flags(boolean isTerminator,
                      boolean isBranch,
                      boolean isCall,
                      boolean isReturn,
                      boolean isPseudo,
                      boolean isCodeGenOnly,
                      boolean mayLoad,
                      boolean mayStore) {
    public static Flags empty() {
      return new Flags(false, false, false, false, false, false, false, false);
    }
  }

  /**
   * Contains information for the lowering of instructions.
   *
   * @param behavior has replaced nodes from {@link ReplaceWithLlvmSDNodesVisitor}.
   * @param inputs   are the input operands for the tablegen instruction.
   * @param outputs  are the output operands for the tablegen instruction.
   * @param flags    are indicators of special properties of the machine instruction.
   * @param patterns are a list of {@link Graph} which contain the pattern selectors for the
   *                 tablegen instruction.
   * @param uses     a list of {@link Register} which are read.
   * @param defs     a list of {@link Register} which are written but are not part of the
   *                 {@code outputs}
   */
  public record LlvmLoweringIntermediateResult(Graph behavior,
                                               List<TableGenInstructionOperand> inputs,
                                               List<TableGenInstructionOperand> outputs,
                                               Flags flags,
                                               List<TableGenPattern> patterns,
                                               List<Register> uses,
                                               List<Register> defs) {

  }

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    IdentityHashMap<Instruction, Graph> uninlined =
        (IdentityHashMap<Instruction, Graph>) passResults.lastResultOf(FunctionInlinerPass.class);
    ensureNonNull(uninlined, "Inlined Function data must exist");
    IdentityHashMap<Instruction, LlvmLoweringIntermediateResult>
        llvmPatterns = new IdentityHashMap<>();
    var supportedInstructions =
        (HashMap<InstructionLabel, List<Instruction>>) passResults
            .lastResultOf(IsaMatchingPass.class);
    ensure(supportedInstructions != null, "Cannot find pass results from IsaMatchPass");

    var instructionLookup = flipIsaMatching(supportedInstructions);

    viam.isas().flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> {
          var instructionLabel = instructionLookup.get(instruction);

          if (instructionLabel == null) {
            return;
          }

          var uninlinedBehavior = (UninlinedGraph) uninlined.get(instruction);
          ensureNonNull(uninlinedBehavior, "uninlinedBehavior graph must exist");
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
   * However, we would like to check whether {@link LlvmInstructionLoweringStrategy} supports this
   * {@link Instruction} in this pass. That's why we have the flip the hashmap.
   */
  public static IdentityHashMap<Instruction, InstructionLabel> flipIsaMatching(
      Map<InstructionLabel, List<Instruction>> isaMatched) {
    IdentityHashMap<Instruction, InstructionLabel> inverse = new IdentityHashMap<>();

    for (var entry : isaMatched.entrySet()) {
      for (var item : entry.getValue()) {
        inverse.put(item, entry.getKey());
      }
    }

    return inverse;
  }
}
