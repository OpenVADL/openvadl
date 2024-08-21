package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TableGenInstructionImmediateOperand that = (TableGenInstructionImmediateOperand) o;
    return Objects.equals(immediateOperand, that.immediateOperand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), immediateOperand);
  }
}
