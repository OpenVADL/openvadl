package vadl.lcb.passes.llvmLowering;

import static vadl.viam.ViamError.ensureNonNull;
import static vadl.viam.ViamError.ensurePresent;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.LcbConfiguration;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.LlvmPseudoInstructionLowerStrategy;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringAddImmediateStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalBranchesStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringConditionalsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringDivisionAndRemainderStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringIndirectJumpStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryLoadStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringMemoryStoreStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmInstructionLoweringUnconditionalJumpsStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringDefaultStrategyImpl;
import vadl.lcb.passes.llvmLowering.strategies.instruction.LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.Abi;

/**
 * This is a wrapper class which contains utility functions for the lowering.
 */
public class LlvmLoweringPass extends Pass {
  public LlvmLoweringPass(LcbConfiguration configuration) {
    super(configuration);
  }

  /**
   * A {@link TableGenInstruction} has many boolean flags which are required for the
   * code generation.
   */
  public record Flags(boolean isTerminator, boolean isBranch, boolean isCall, boolean isReturn,
                      boolean isPseudo, boolean isCodeGenOnly, boolean mayLoad, boolean mayStore) {
    public static Flags empty() {
      return new Flags(false, false, false, false, false, false, false, false);
    }

    /**
     * Given {@link Flags} overwrite the {@code isTerminator} and return it.
     */
    public static Flags withTerminator(Flags flags) {
      return new Flags(true, flags.isBranch, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore());
    }

    /**
     * Given {@link Flags} overwrite the {@code isBranch} and return it.
     */
    public static Flags withBranch(Flags flags) {
      return new Flags(flags.isTerminator(), true, flags.isCall, flags.isReturn, flags.isPseudo,
          flags.isCodeGenOnly, flags.mayLoad, flags.mayStore());
    }
  }

  /**
   * This is the result of the {@link LlvmLoweringPass}. It contains the
   * tablegen records for machine instructions and pseudo instructions.
   */
  public record LlvmLoweringPassResult(
      IdentityHashMap<Instruction, LlvmLoweringRecord> machineInstructionRecords,
      IdentityHashMap<PseudoInstruction, LlvmLoweringRecord> pseudoInstructionRecords) {

  }

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var labelledMachineInstructions = ensureNonNull(
        (Map<MachineInstructionLabel, List<Instruction>>) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.sourceLocation()));
    var labelledPseudoInstructions = ensureNonNull(
        (Map<PseudoInstructionLabel, List<PseudoInstruction>>) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.sourceLocation()));
    var abi = (Abi) viam.definitions().filter(x -> x instanceof Abi).findFirst().get();

    var architectureType =
        ensurePresent(ValueType.from(abi.stackPointer().registerFile().resultType()),
            "Architecture type is required.");
    var machineStrategies =
        List.of(new LlvmInstructionLoweringAddImmediateStrategyImpl(architectureType),
            new LlvmInstructionLoweringDivisionAndRemainderStrategyImpl(architectureType),
            new LlvmInstructionLoweringConditionalsStrategyImpl(architectureType),
            new LlvmInstructionLoweringUnconditionalJumpsStrategyImpl(architectureType),
            new LlvmInstructionLoweringConditionalBranchesStrategyImpl(architectureType),
            new LlvmInstructionLoweringIndirectJumpStrategyImpl(architectureType),
            new LlvmInstructionLoweringMemoryStoreStrategyImpl(architectureType),
            new LlvmInstructionLoweringMemoryLoadStrategyImpl(architectureType),
            new LlvmInstructionLoweringDefaultStrategyImpl(architectureType));
    var pseudoStrategies =
        List.of(new LlvmPseudoInstructionLoweringUnconditionalJumpsStrategyImpl(machineStrategies),
            new LlvmPseudoInstructionLoweringDefaultStrategyImpl(machineStrategies));

    var machineRecords = generateRecordsForMachineInstructions(viam, abi, machineStrategies,
        labelledMachineInstructions);
    var pseudoRecords = pseudoInstructions(viam, abi, pseudoStrategies,
        labelledMachineInstructions, labelledPseudoInstructions);

    return new LlvmLoweringPassResult(machineRecords, pseudoRecords);
  }


  private IdentityHashMap<Instruction, LlvmLoweringRecord> generateRecordsForMachineInstructions(
      Specification viam, Abi abi, List<LlvmInstructionLoweringStrategy> strategies,
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions) {
    var tableGenRecords = new IdentityHashMap<Instruction, LlvmLoweringRecord>();

    // We flip it because we need to know the label for the instruction to
    // apply one of the different lowering strategies.
    // A strategy knows whether it can lower it by the label.
    var instructionLookup = flipMachineInstructions(labelledMachineInstructions);

    viam.isa().map(isa -> isa.ownInstructions().stream()).orElseGet(Stream::empty)
        .forEach(instruction -> {
          var instructionLabel = instructionLookup.get(instruction);

          for (var strategy : strategies) {
            if (!strategy.isApplicable(instructionLabel)) {
              // Try next strategy
              continue;
            }

            var record =
                strategy.lower(labelledMachineInstructions, instruction, instruction.behavior(),
                    abi);

            // Okay, we have to save record.
            record.ifPresent(llvmLoweringIntermediateResult -> tableGenRecords.put(instruction,
                llvmLoweringIntermediateResult));

            // Allow only one strategy to apply.
            // Otherwise, the results from a previous strategy are overwritten.
            break;
          }
        });

    return tableGenRecords;
  }

  private IdentityHashMap<PseudoInstruction, LlvmLoweringRecord> pseudoInstructions(
      Specification viam, Abi abi, List<LlvmPseudoInstructionLowerStrategy> pseudoStrategies,
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Map<PseudoInstructionLabel, List<PseudoInstruction>> labelledPseudoInstructions) {
    var tableGenRecords = new IdentityHashMap<PseudoInstruction, LlvmLoweringRecord>();
    var flipped = flipPseudoInstructions(labelledPseudoInstructions);

    viam.isa().map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .forEach(pseudo -> {
          for (var strategy : pseudoStrategies) {
            var label = flipped.get(pseudo);
            if (!strategy.isApplicable(label)) {
              continue;
            }

            var record = strategy.lower(abi, pseudo, labelledMachineInstructions);

            // Okay, we have to save record.
            record.ifPresent(llvmLoweringIntermediateResult -> tableGenRecords.put(pseudo,
                llvmLoweringIntermediateResult));

            break;
          }
        });

    return tableGenRecords;
  }

  /**
   * The {@link IsaMachineInstructionMatchingPass} computes a hashmap with the instruction label
   * as a key and all the matched instructions as value.
   * However, we would like to check whether {@link LlvmInstructionLoweringStrategy} supports this
   * {@link Instruction} in this pass. That's why we have the flip the hashmap.
   */
  public static IdentityHashMap<Instruction, MachineInstructionLabel> flipMachineInstructions(
      Map<MachineInstructionLabel, List<Instruction>> isaMatched) {
    IdentityHashMap<Instruction, MachineInstructionLabel> inverse = new IdentityHashMap<>();

    for (var entry : isaMatched.entrySet()) {
      for (var item : entry.getValue()) {
        inverse.put(item, entry.getKey());
      }
    }

    return inverse;
  }

  /**
   * The {@link IsaMachineInstructionMatchingPass} computes a hashmap with the instruction label
   * as a key and all the matched instructions as value.
   * However, we would like to check whether {@link LlvmPseudoInstructionLowerStrategy} supports
   * this {@link Instruction} in this pass. That's why we have the flip the hashmap.
   */
  public static IdentityHashMap<PseudoInstruction,
      PseudoInstructionLabel> flipPseudoInstructions(
      Map<PseudoInstructionLabel, List<PseudoInstruction>> isaMatched) {
    IdentityHashMap<PseudoInstruction, PseudoInstructionLabel> inverse = new IdentityHashMap<>();

    for (var entry : isaMatched.entrySet()) {
      for (var item : entry.getValue()) {
        inverse.put(item, entry.getKey());
      }
    }

    return inverse;
  }
}
