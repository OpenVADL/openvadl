package vadl.lcb.passes.llvmLowering.strategies;

import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionImmediateLabelOperand;

/**
 * Utilities for lowering.
 */
public class LoweringStrategyUtils {

  /**
   * Conditional and unconditional branch patterns reference the {@code bb} selection dag node.
   * However, the machine instruction should use the label immediate to properly encode the
   * instruction.
   */
  public static TableGenPattern replaceBasicBlockByLabelImmediateInMachineInstruction(
      TableGenPattern pattern) {

    if (pattern instanceof TableGenSelectionWithOutputPattern) {
      // We know that the `selector` already has LlvmBasicBlock nodes.
      var candidates = ((TableGenSelectionWithOutputPattern) pattern).machine().getNodes(
          LcbMachineInstructionParameterNode.class).toList();
      for (var candidate : candidates) {
        if (candidate.instructionOperand().origin() instanceof LlvmBasicBlockSD basicBlockSD) {
          candidate.setInstructionOperand(
              new TableGenInstructionImmediateLabelOperand(basicBlockSD));
        }
      }
    }

    return pattern;
  }
}
