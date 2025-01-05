package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.Format;
import vadl.viam.RegisterFile;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link RegisterFile} when the address is a {@link Format.Field}.
 */
public class TableGenInstructionRegisterFileOperand extends TableGenInstructionOperand
    implements ReferencesFormatField {
  private final RegisterFile registerFile;
  private final Format.Field formatField;
  private final Node reference;

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(ReadRegFileNode node, FieldRefNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.formatField().identifier.simpleName()));
    this.registerFile = node.registerFile();
    this.formatField = address.formatField();
    this.reference = node;
  }

  /**
   * Constructor.
   */
  public TableGenInstructionRegisterFileOperand(WriteRegFileNode node, FieldRefNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.formatField().identifier.simpleName()));
    this.registerFile = node.registerFile();
    this.formatField = address.formatField();
    this.reference = node;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  @Override
  public Format.Field formatField() {
    return formatField;
  }

  public Node reference() {
    return reference;
  }
}
