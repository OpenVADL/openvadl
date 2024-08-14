package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

public class LlvmSRemSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmSRemSD(NodeList<ExpressionNode> args,
                    Type type) {
    super(BuiltInTable.SMOD, args, type);
  }

  @Override
  public String lower() {
    return "srem";
  }
}
