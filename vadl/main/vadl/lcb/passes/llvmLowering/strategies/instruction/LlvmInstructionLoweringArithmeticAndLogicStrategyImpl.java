package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.isaMatching.InstructionLabel.ADDI_32;
import static vadl.lcb.passes.isaMatching.InstructionLabel.ADDI_64;
import static vadl.lcb.passes.isaMatching.InstructionLabel.ADD_32;
import static vadl.lcb.passes.isaMatching.InstructionLabel.ADD_64;
import static vadl.lcb.passes.isaMatching.InstructionLabel.AND;
import static vadl.lcb.passes.isaMatching.InstructionLabel.MUL;
import static vadl.lcb.passes.isaMatching.InstructionLabel.OR;
import static vadl.lcb.passes.isaMatching.InstructionLabel.SDIV;
import static vadl.lcb.passes.isaMatching.InstructionLabel.SMOD;
import static vadl.lcb.passes.isaMatching.InstructionLabel.SUB;
import static vadl.lcb.passes.isaMatching.InstructionLabel.SUBB;
import static vadl.lcb.passes.isaMatching.InstructionLabel.SUBC;
import static vadl.lcb.passes.isaMatching.InstructionLabel.UDIV;
import static vadl.lcb.passes.isaMatching.InstructionLabel.UMOD;
import static vadl.lcb.passes.isaMatching.InstructionLabel.XOR;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowers arithmetic and logic instructions into {@link TableGenInstruction}.
 */
public class LlvmInstructionLoweringArithmeticAndLogicStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  private final Set<InstructionLabel> supported = Set.of(ADD_32,
      ADD_64, ADDI_32, ADDI_64, AND, OR, SUB, MUL, SUBB, SUBC, SDIV, UDIV, SMOD, UMOD, XOR
  );

  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return supported;
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      UninlinedGraph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
