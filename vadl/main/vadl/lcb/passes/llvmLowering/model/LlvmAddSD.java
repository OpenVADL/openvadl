package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

public class LlvmAddSD extends BuiltInCall implements LlvmNodeLowerable {

  public LlvmAddSD(NodeList<ExpressionNode> args,
                   Type type) {
    super(BuiltInTable.ADD, args, type);
  }

  @Override
  public String lower() {
    return "add";
  }
}
