package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.viam.Register;

/**
 * Indicates that the operand is a {@link Register} which is the frame pointer.
 */
public class TableGenInstructionFrameRegisterOperand extends TableGenInstructionOperand {
  public TableGenInstructionFrameRegisterOperand(String name, LlvmFrameIndexSD node) {
    super(node, LlvmFrameIndexSD.NAME, name);
  }
}
