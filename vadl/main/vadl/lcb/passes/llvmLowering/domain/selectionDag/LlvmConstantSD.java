package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.viam.Constant;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * LLVM Node for a constant value.
 */
public class LlvmConstantSD extends ConstantNode implements LlvmNodeLowerable {

  public LlvmConstantSD(Constant constant) {
    super(constant);
  }

  @Override
  public String lower() {
    return constant.asVal().toString();
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

  @Override
  public Node copy() {
    return new LlvmConstantSD(constant);
  }

  @Override
  public Node shallowCopy() {
    return new LlvmConstantSD(constant);
  }
}
