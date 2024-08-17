package vadl.lcb.tablegen.model;

/**
 * Indicates that the operand is an immediate.
 */
public class TableGenInstructionImmediateOperand extends TableGenInstructionOperand {
  private final TableGenImmediateOperand immediateOperand;

  public TableGenInstructionImmediateOperand(String type, String name,
                                             TableGenImmediateOperand immediateOperand) {
    super(type, name);
    this.immediateOperand = immediateOperand;
  }

  public TableGenImmediateOperand immediateOperand() {
    return immediateOperand;
  }
}
