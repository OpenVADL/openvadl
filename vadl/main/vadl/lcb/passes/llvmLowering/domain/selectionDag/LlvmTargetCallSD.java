package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.dependency.AbstractFunctionCallNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM Node for target_call.
 */
public class LlvmTargetCallSD extends AbstractFunctionCallNode implements LlvmNodeLowerable {

  public LlvmTargetCallSD(NodeList<ExpressionNode> args,
                          Type type) {
    super(args, type);
  }

  @Override
  public String lower() {
    return "target_call";
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
    return new LlvmTargetCallSD(new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmTargetCallSD(args, type());
  }
}
