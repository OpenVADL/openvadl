package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoLoweringImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringAddImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringArithmeticAndLogicStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringIndirectJumpStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryLoadStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryStoreStrategyImpl;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
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
   * This is the result of the {@link LlvmLoweringPass}. It contains the
   * tablegen records for machine instructions and pseudo instructions.
   */
  public record LlvmLoweringPassResult(
      IdentityHashMap<Instruction, LlvmLoweringRecord> machineInstructionRecords,
      IdentityHashMap<PseudoInstruction, LlvmLoweringRecord> pseudoInstructionRecords
  ) {

  }

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {

    var machineRecords = generateRecordsForMachineInstructions(passResults, viam);
    var pseudoRecords = generateRecordsForPseudoInstructions(viam);

    return new LlvmLoweringPassResult(machineRecords, pseudoRecords);
  }


  private IdentityHashMap<Instruction, LlvmLoweringRecord>
  generateRecordsForMachineInstructions(
      PassResults passResults,
      Specification viam
  ) {
    var tableGenRecords = new IdentityHashMap<Instruction, LlvmLoweringRecord>();

    // Get the supported instructions from the matching.
    // We only instructions which we know about in this pass.
    // TODO: define a strategy as fallback when there is no matching.
    var supportedInstructions =
        (HashMap<InstructionLabel, List<Instruction>>) passResults
            .lastResultOf(IsaMatchingPass.class);
    ensure(supportedInstructions != null, "Cannot find pass results from IsaMatchPass");
    var uninlined =
        (IdentityHashMap<Instruction, Graph>) passResults.lastResultOf(FunctionInlinerPass.class);
    ensureNonNull(uninlined, "Inlined Function data must exist");

    // We flip it because we need to know the label for the instruction to
    // apply one of the different lowering strategies.
    // A strategy knows whether it can lower it by the label.
    var instructionLookup = flipIsaMatching(supportedInstructions);

    viam.isa().map(isa -> isa.ownInstructions().stream())
        .orElseGet(Stream::empty)
        .forEach(instruction -> {
          var instructionLabel = instructionLookup.get(instruction);

          // TODO: No label, then we need to have a default.
          if (instructionLabel == null) {
            DeferredDiagnosticStore.add(Diagnostic.warning(
                "Instruction was not matched. Therefore, it will be skipped for the compiler"
                    + " lowering (todo)",
                instruction.sourceLocation()).build());
            return;
          }

          var uninlinedBehavior = (UninlinedGraph) uninlined.get(instruction);
          ensureNonNull(uninlinedBehavior, "uninlinedBehavior graph must exist");
          for (var strategy : strategies) {
            if (!strategy.isApplicable(instructionLabel)) {
              // Try next strategy
              continue;
            }

            var record =
                strategy.lower(supportedInstructions, instruction, instructionLabel,
                    uninlinedBehavior);

            // Okay, we have to save record.
            record.ifPresent(llvmLoweringIntermediateResult -> tableGenRecords.put(instruction,
                llvmLoweringIntermediateResult));
          }
        });

    return tableGenRecords;
  }

  private IdentityHashMap<PseudoInstruction, LlvmLoweringRecord> generateRecordsForPseudoInstructions(
      Specification viam) {
    var tableGenRecords = new IdentityHashMap<PseudoInstruction, LlvmLoweringRecord>();

    viam.isa().map(isa -> isa.ownPseudoInstructions().stream())
        .orElseGet(Stream::empty)
        .forEach(pseudo -> {
          var record = new LlvmPseudoLoweringImpl().lower(pseudo);

          // Okay, we have to save record.
          record.ifPresent(llvmLoweringIntermediateResult -> tableGenRecords.put(pseudo,
              llvmLoweringIntermediateResult));
        });

    return tableGenRecords;
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
