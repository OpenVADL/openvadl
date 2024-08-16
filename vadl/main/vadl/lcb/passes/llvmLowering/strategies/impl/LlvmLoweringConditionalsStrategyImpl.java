package vadl.lcb.passes.llvmLowering.strategies.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

public class LlvmLoweringConditionalsStrategyImpl extends LlvmLoweringStrategy {

  private final Set<InstructionLabel> supported = Set.of(InstructionLabel.LT);

  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return this.supported;
  }

  @Override
  protected List<LlvmLoweringPass.LlvmLoweringTableGenPattern> generatePatternVariations(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      Graph copy,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<Graph> patterns) {
    return Collections.emptyList();
  }
}
