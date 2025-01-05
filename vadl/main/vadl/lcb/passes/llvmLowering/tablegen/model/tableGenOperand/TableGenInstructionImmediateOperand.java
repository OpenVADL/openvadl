package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import java.util.Objects;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.ReferencesFormatField;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameter;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.Format;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Indicates that the operand is an immediate.
 */
public class TableGenInstructionImmediateOperand extends TableGenInstructionOperand
    implements ReferencesFormatField {
  private final TableGenImmediateRecord immediateOperand;

  public TableGenInstructionImmediateOperand(LlvmFieldAccessRefNode node) {
    super(node, new TableGenParameterTypeAndName(node.immediateOperand().fullname(),
        node.fieldAccess().fieldRef().identifier.simpleName()));
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
