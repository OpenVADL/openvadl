package vadl.lcb.passes.llvm_lowering.model;

import vadl.lcb.passes.llvm_lowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

public class LlvmSubSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmSubSD(NodeList<ExpressionNode> args,
                   Type type) {
    super(BuiltInTable.SUB, args, type);
  }

  @Override
  public String lower() {
    return "sub";
  }
}
