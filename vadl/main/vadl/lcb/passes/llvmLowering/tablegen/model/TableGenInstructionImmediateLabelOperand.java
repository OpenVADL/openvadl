package vadl.lcb.passes.llvmLowering.tablegen.model;

import static vadl.viam.ViamError.ensure;

import java.util.Objects;
import vadl.error.Diagnostic;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.viam.Format;

/**
 * Indicates that the operand is an immediate but as a label.
 */
public class TableGenInstructionImmediateLabelOperand extends TableGenInstructionOperand
    implements ReferencesFormatField {
  private final TableGenImmediateRecord immediateOperand;

  public TableGenInstructionImmediateLabelOperand(ParameterIdentity identity,
                                                  LlvmBasicBlockSD node) {
    super(node, identity);
    this.immediateOperand = node.immediateOperand();
  }

  public TableGenInstructionImmediateLabelOperand(ParameterIdentity identity,
                                                  LlvmFieldAccessRefNode node) {
    super(node, identity);
    ensure(node.usage() == LlvmFieldAccessRefNode.Usage.BasicBlock,
        () -> Diagnostic.error(
            "Field reference has wrong type. It is expected to be basic block but it is not.",
            node.sourceLocation()));
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

  @Override
  public Format.Field formatField() {
    return immediateOperand.fieldAccessRef().fieldRef();
  }
}
