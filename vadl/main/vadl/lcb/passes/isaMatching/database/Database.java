package vadl.lcb.passes.isaMatching.database;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.error.Diagnostic;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This database contains the labelled instructions and pseudo instructions and makes it possible
 * to query for instructions.
 */
public class Database {
  private final Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions;
  private final Map<PseudoInstructionLabel, List<PseudoInstruction>> labelledPseudoInstructions;

  /**
   * Constructor. It requires the information from {@link IsaMachineInstructionMatchingPass} and
   * {@link IsaPseudoInstructionMatchingPass} to have labelled instructions and pseudo instructions.
   */
  public Database(PassResults passResults, Specification viam) {
    var labelingResult = ensureNonNull(
        (IsaMachineInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.sourceLocation()));
    var labelingPseudoResult = ensureNonNull(
        (IsaPseudoInstructionMatchingPass.Result) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions", viam.sourceLocation()));
    this.labelledMachineInstructions = labelingResult.labels();
    this.labelledPseudoInstructions = labelingPseudoResult.labels();
  }

  /**
   * Constructor for {@link Database}.
   */
  public Database(IsaMachineInstructionMatchingPass.Result labelingResult) {
    this.labelledMachineInstructions = labelingResult.labels();
    this.labelledPseudoInstructions = Collections.emptyMap();
  }
  /**
   * Run the given {@link Query} and return the matched {@link Instruction} and
   * {@link PseudoInstruction} wrapped by {@link QueryResult}.
   * Note that the query will be executed on both types of instructions. Therefore, when
   * given a {@link MachineInstructionLabel} and {@link PseudoInstructionLabel} then you might get
   * two results and not the intersection of both.
   */
  public QueryResult run(Query query) {
    var result = matchInstructions(query);

    return new QueryResult(query, result.machineInstructions(), result.pseudoInstructions());
  }

  private QueryResult matchInstructions(Query query) {
    var resultMachineInstructions = new ArrayList<Instruction>();
    var resultPseudoInstructions = new ArrayList<PseudoInstruction>();

    if (query.machineInstructionLabel() != null) {
      resultMachineInstructions.addAll(
          labelledMachineInstructions.getOrDefault(query.machineInstructionLabel(),
              Collections.emptyList()));
    }

    if (query.pseudoInstructionLabel() != null) {
      resultPseudoInstructions.addAll(
          labelledPseudoInstructions.getOrDefault(query.pseudoInstructionLabel(),
              Collections.emptyList()));
    }

    if (query.machineInstructionLabelGroup() != null) {
      var labels = query.machineInstructionLabelGroup().labels();
      for (var label : labels) {
        var instruction = labelledMachineInstructions.get(label);
        if (instruction != null) {
          resultMachineInstructions.addAll(instruction);
        }
      }
    }

    for (var x : query.or()) {
      var subResult = matchInstructions(x);
      resultMachineInstructions.addAll(subResult.machineInstructions());
      resultPseudoInstructions.addAll(subResult.pseudoInstructions());
    }

    for (var x : query.withBehavior()) {
      // Remove machine instruction when any behavior query matches.
      resultMachineInstructions.removeIf(instruction -> {
        var satisfied = instruction.behavior().getNodes()
            .filter(node -> x.applicable().isInstance(node))
            .allMatch(node -> x.predicate().test(node));

        // only remove when it's not covered.
        return !satisfied;
      });
    }

    return new QueryResult(query, resultMachineInstructions, resultPseudoInstructions);
  }


  /**
   * The compiler generator has a pass which tries to assign {@link MachineInstructionLabel} for
   * an {@link Instruction}. This is useful when we want to find an {@link Instruction} with
   * a certain property. However, in some cases, we need to do opposite. We have an
   * {@link Instruction} and require the {@link MachineInstructionLabel}. This method flips the
   * matched {@link Map}.
   */
  public IdentityHashMap<Instruction, MachineInstructionLabel> flipMachineInstructions() {
    return LlvmLoweringPass.flipMachineInstructions(labelledMachineInstructions);
  }

  /**
   * The compiler generator has a pass which tries to assign {@link PseudoInstructionLabel} for
   * an {@link PseudoInstruction}. This is useful when we want to find an {@link PseudoInstruction}
   * with a certain property. However, in some cases, we need to do opposite. We have an
   * {@link PseudoInstruction} and require the {@link PseudoInstructionLabel}. This method flips the
   * matched {@link Map}.
   */
  public IdentityHashMap<PseudoInstruction,
      PseudoInstructionLabel> flipPseudoInstructions() {
    return LlvmLoweringPass.flipPseudoInstructions(labelledPseudoInstructions);
  }
}
