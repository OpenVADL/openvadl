package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.visitors.MachineInstructionLcbVisitor;
import vadl.lcb.tablegen.lowering.TableGenPatternVisitor;
import vadl.lcb.tablegen.model.TableGenImmediateOperand;
import vadl.lcb.visitors.LcbGraphNodeVisitor;
import vadl.types.Type;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
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
        new TableGenImmediateOperand(fieldAccess.accessFunction().identifier.lower(), ValueType.from(type));
  }

  public TableGenImmediateOperand immediateOperand() {
    return immediateOperand;
  }


  @Override
  public void accept(GraphNodeVisitor visitor) {
    if (visitor instanceof MachineInstructionLcbVisitor v) {
      v.visit(this);
    } else {
      visitor.visit(this);
    }
  }
}
