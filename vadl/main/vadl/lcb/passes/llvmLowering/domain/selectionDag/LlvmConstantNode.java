package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * TableGen requires to have matching variable names even when the constant replaced.
 * This class extends the {@link ConstantNode} to contain the original value.
 */
public class LlvmConstantNode extends ConstantNode {
  private final Format.Field replacedField;

  public LlvmConstantNode(Constant constant, Format.Field replacedField) {
    super(constant);
    this.replacedField = replacedField;
  }

  @Override
  public Node copy() {
    return new LlvmConstantNode(constant, replacedField);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmConstantNode(constant, replacedField);
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

  public Format.Field replacedField() {
    return replacedField;
  }
}
