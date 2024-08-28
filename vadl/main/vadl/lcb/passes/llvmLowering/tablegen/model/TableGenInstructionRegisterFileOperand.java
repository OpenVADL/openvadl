package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.Format;
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link RegisterFile}.
 */
public class TableGenInstructionRegisterFileOperand extends TableGenInstructionOperand {
  private final RegisterFile registerFile;
  private final Format.Field formatField;

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(String type,
                                                String name,
                                                ReadRegFileNode node,
                                                Format.Field formatField) {
    super(node, type, name);
    this.registerFile = node.registerFile();
    this.formatField = formatField;
  }

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(String type,
                                                String name,
                                                WriteRegFileNode node,
                                                Format.Field formatField) {
    super(node, type, name);
    this.registerFile = node.registerFile();
    this.formatField = formatField;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  public Format.Field formatField() {
    return formatField;
  }
}
