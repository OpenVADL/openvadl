package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.Register;

/**
 * Indicates that the operand is a {@link Register}.
 */
public class TableGenInstructionRegisterOperand extends TableGenInstructionOperand {
  public TableGenInstructionRegisterOperand(String type, String name) {
    super(type, name);
  }
}
