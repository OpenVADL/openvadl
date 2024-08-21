package vadl.lcb.passes.llvmLowering.strategies.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowering of conditionals into TableGen.
 */
public class LlvmLoweringConditionalsStrategyImpl extends LlvmLoweringStrategy {

  private final Set<InstructionLabel> supported = Set.of(InstructionLabel.LT);

  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return this.supported;
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      UninlinedGraph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
