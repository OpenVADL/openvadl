package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;

/**
 * Lowers instructions into {@link TableGenInstruction}.
 */
public class LlvmInstructionLoweringDefaultStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Collections.emptySet();
  }

  @Override
  public boolean isApplicable(@Nullable InstructionLabel instructionLabel) {
    // Accept every label.
    return true;
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
