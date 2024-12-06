package vadl.lcb.passes.isaMatching.database;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import vadl.error.Diagnostic;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.IsaPseudoInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.isaMatching.PseudoInstructionLabel;
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
    var labelledMachineInstructions = ensureNonNull(
        (Map<MachineInstructionLabel, List<Instruction>>) passResults.lastResultOf(
            IsaMachineInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions",
            viam.sourceLocation()));
    var labelledPseudoInstructions = ensureNonNull(
        (Map<PseudoInstructionLabel, List<PseudoInstruction>>) passResults.lastResultOf(
            IsaPseudoInstructionMatchingPass.class),
        () -> Diagnostic.error("Cannot find semantics of the instructions",
            viam.sourceLocation()));

    this.labelledMachineInstructions = labelledMachineInstructions;
    this.labelledPseudoInstructions = labelledPseudoInstructions;
  }

  /**
   * Run the given {@link Query} and return the matched {@link Instruction} and
   * {@link PseudoInstruction} wrapped by {@link QueryResult}.
   * Note that the query will be executed on both types of instructions. Therefore, when
   * given a {@link MachineInstructionLabel} and {@link PseudoInstructionLabel} then you might get
   * two results and not the intersection of both.
   */
  public QueryResult run(Query query) {
    var instructions = matchInstructions(query);
    var pseudoInstructions = matchPseudoInstructions(query);

    return new QueryResult(instructions, pseudoInstructions);
  }

  private List<Instruction> matchInstructions(Query query) {
    if (query.machineInstructionLabel() != null) {
      return labelledMachineInstructions.getOrDefault(query.machineInstructionLabel(),
          Collections.emptyList());
    }

    return Collections.emptyList();
  }


  private List<PseudoInstruction> matchPseudoInstructions(Query query) {
    if (query.pseudoInstructionLabel() != null) {
      return labelledPseudoInstructions.getOrDefault(query.pseudoInstructionLabel(),
          Collections.emptyList());
    }

    return Collections.emptyList();
  }
}
