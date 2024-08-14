package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

public class LlvmAndSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmAndSD(NodeList<ExpressionNode> args,
                   Type type) {
    super(BuiltInTable.AND, args, type);
  }

  @Override
  public String lower() {
    return "and";
  }
}
