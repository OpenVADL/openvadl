package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;

/**
 * Indicates that the operand is an immediate but as a label.
 */
public class TableGenInstructionImmediateLabelOperand extends TableGenInstructionOperand {
  private final TableGenImmediateRecord immediateOperand;

  public TableGenInstructionImmediateLabelOperand(ParameterIdentity identity,
                                                  LlvmBasicBlockSD node) {
    super(node, identity);
    this.immediateOperand = node.immediateOperand();
  }

  public TableGenImmediateRecord immediateOperand() {
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
    TableGenInstructionImmediateLabelOperand that = (TableGenInstructionImmediateLabelOperand) o;
    return Objects.equals(immediateOperand, that.immediateOperand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), immediateOperand);
  }
}
