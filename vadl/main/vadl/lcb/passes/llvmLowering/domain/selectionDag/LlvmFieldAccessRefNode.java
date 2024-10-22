package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;

/**
 * This class represents a field access in LLVM. It extends {@link FieldAccessRefNode} because
 * it requires additional information for rendering an immediate.
 */
public class LlvmFieldAccessRefNode extends FieldAccessRefNode {
  private final ValueType llvmType;
  private final TableGenImmediateRecord immediateOperand;
  protected final ParameterIdentity parameterIdentity;

  /**
   * Creates an {@link LlvmFieldAccessRefNode} object that holds a reference to a format field
   * access.
   *
   * @param fieldAccess  the format immediate to be referenced
   * @param originalType of the node. This type might not be correctly sized because vadl allows
   *                     arbitrary bit sizes.
   * @param llvmType     is same as {@code originalType} when it is a valid LLVM type. Otherwise,
   *                     it is the next upcasted type.
   */
  public LlvmFieldAccessRefNode(Format.FieldAccess fieldAccess,
                                Type originalType,
                                ValueType llvmType) {
    super(fieldAccess, originalType);
    this.immediateOperand =
        new TableGenImmediateRecord(fieldAccess, llvmType);
    this.parameterIdentity = ParameterIdentity.from(this);
    this.llvmType = llvmType;
  }

  @Override
  public Node copy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type(), llvmType);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type(), llvmType);
  }

  public TableGenImmediateRecord immediateOperand() {
    return immediateOperand;
  }


  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof TableGenMachineInstructionVisitor v) {
      v.visit(this);
    } else if (visitor instanceof TableGenNodeVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }
}
