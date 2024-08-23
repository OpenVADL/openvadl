package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateOperand;
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
  private final TableGenImmediateOperand immediateOperand;

  /**
   * Creates an {@link LlvmFieldAccessRefNode} object that holds a reference to a format field
   * access.
   *
   * @param fieldAccess the format immediate to be referenced
   * @param type        of the node.
   */
  public LlvmFieldAccessRefNode(Format.FieldAccess fieldAccess, Type type) {
    super(fieldAccess, type);
    this.immediateOperand =
        new TableGenImmediateOperand(fieldAccess.accessFunction().identifier.lower(),
            ValueType.from(type));
  }

  @Override
  public Node copy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmFieldAccessRefNode(fieldAccess, type());
  }

  public TableGenImmediateOperand immediateOperand() {
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
