package vadl.lcb.passes.llvm_lowering.model;

import vadl.lcb.passes.llvm_lowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

public class LlvmMulSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmMulSD(NodeList<ExpressionNode> args,
                   Type type) {
    super(BuiltInTable.MUL, args, type);
  }

  @Override
  public String lower() {
    return "mul";
  }
}
