package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.Format;
import vadl.viam.RegisterFile;

/**
 * Indicates that the operand is a {@link RegisterFile}.
 */
public class TableGenInstructionRegisterFileOperand extends TableGenInstructionOperand {
  private final RegisterFile registerFile;
  private final Format.Field formatField;

  public TableGenInstructionRegisterFileOperand(String type, String name,
                                                RegisterFile registerFile,
                                                Format.Field formatField) {
    super(type, name);
    this.registerFile = registerFile;
    this.formatField = formatField;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  public Format.Field formatField() {
    return formatField;
  }
}
