package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;


/**
 * LLVM node for multiplication.
 */
public class LlvmMulSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmMulSD(NodeList<ExpressionNode> args,
                   Type type) {
    super(BuiltInTable.MUL, args, type);
  }

  @Override
  public String lower() {
    return "mul";
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
    return new LlvmMulSD(new NodeList<>(args.stream().map(x -> (ExpressionNode) x.copy()).toList()),
        type());
  }

  @Override
  public Node shallowCopy() {
    return new LlvmMulSD(args, type());
  }
}
