package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.viam.Format;
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link RegisterFile} when the address is a {@link Format.Field}.
 */
public class TableGenInstructionRegisterFileOperand extends TableGenInstructionOperand
    implements ReferencesFormatField {
  private final RegisterFile registerFile;
  private final Format.Field formatField;

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(ParameterIdentity identity,
                                                ReadRegFileNode node,
                                                Format.Field formatField) {
    super(node, identity);
    this.registerFile = node.registerFile();
    this.formatField = formatField;
  }

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(ParameterIdentity identity,
                                                WriteRegFileNode node,
                                                Format.Field formatField) {
    super(node, identity);
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
