package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.viam.Format;

/**
 * Indicates that the operand is an immediate.
 */
public class TableGenInstructionImmediateOperand extends TableGenInstructionOperand
    implements ReferencesFormatField {
  private final TableGenImmediateRecord immediateOperand;

  public TableGenInstructionImmediateOperand(ParameterIdentity identity,
                                             LlvmFieldAccessRefNode node) {
    super(node, identity);
    this.immediateOperand = node.immediateOperand();
  }


  public TableGenInstructionImmediateOperand(ParameterIdentity identity,
                                             TableGenImmediateRecord immediateRecord) {
    super(null, identity);
    this.immediateOperand = immediateRecord;
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
    TableGenInstructionImmediateOperand that = (TableGenInstructionImmediateOperand) o;
    return Objects.equals(immediateOperand, that.immediateOperand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), immediateOperand);
  }

  @Override
  public Format.Field formatField() {
    return immediateOperand.fieldAccessRef().fieldRef();
  }
}
