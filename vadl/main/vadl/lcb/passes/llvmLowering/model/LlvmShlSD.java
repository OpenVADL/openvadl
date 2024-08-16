package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Logical node for logical shift left.
 */
public class LlvmShlSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmShlSD(NodeList<ExpressionNode> args,
                   Type type) {
    super(BuiltInTable.LSL, args, type);
  }

  @Override
  public String lower() {
    return "shl";
  }
}
