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
  private final Usage usage;

  /**
   * Indicates how the field is used. It is {@code Immediate} when the field is directly as
   * immediate. However, it is {@code BasicBlock} if the field is a symbol to a basic block.
   * This indicates to TableGen that the type is {@code OtherVT}.
   */
  public enum Usage {
    Immediate,
    BasicBlock
  }

  /**
   * Creates an {@link LlvmFieldAccessRefNode} object that holds a reference to a format field
   * access.
   *
   * @param fieldAccess  the format immediate to be referenced
   * @param originalType of the node. This type might not be correctly sized because vadl allows
   *                     arbitrary bit sizes.
   * @param llvmType     is same as {@code originalType} when it is a valid LLVM type. Otherwise,
   *                     it is the next upcasted type.
   * @param usage        indicates how the field is used.
   */
  public LlvmFieldAccessRefNode(Format.FieldAccess fieldAccess,
                                Type originalType,
                                ValueType llvmType,
                                Usage usage) {
    super(fieldAccess, originalType);
    this.immediateOperand =
        new TableGenImmediateRecord(fieldAccess, llvmType);
    this.parameterIdentity = usage == Usage.Immediate ? ParameterIdentity.from(this) :
        ParameterIdentity.fromToImmediateLabel(this);
    this.llvmType = llvmType;
    this.usage = usage;
  }

  @Override
  public Node copy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type(), llvmType, usage);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type(), llvmType, usage);
  }

  public TableGenImmediateRecord immediateOperand() {
    return immediateOperand;
  }

  public Usage usage() {
    return usage;
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
