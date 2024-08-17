package vadl.lcb.passes.llvmLowering.strategies.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass.LlvmLoweringIntermediateResult;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

/**
 * Generates the {@link LlvmLoweringIntermediateResult} for {@link InstructionLabel#JALR}
 * instruction.
 */
public class LlvmLoweringIndirectJumpStrategyImpl extends LlvmLoweringStrategy {
  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Set.of(InstructionLabel.JALR);
  }

  @Override
  public Optional<LlvmLoweringIntermediateResult> lower(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions, Instruction instruction,
      InstructionLabel instructionLabel, Graph uninlinedBehavior) {
    var copy = uninlinedBehavior.copy();
    var visitor = getVisitorForPatternSelectorLowering();

    for (var node : copy.getNodes().toList()) {
      visitor.visit(node);
    }

    return Optional.of(new LlvmLoweringIntermediateResult(
        copy,
        getTableGenInputOperands(copy),
        Collections.emptyList(), // expecting no outputs
        Collections.emptyList() // TODO: currently do not generate indirect call
    ));
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel, Graph copy, List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
