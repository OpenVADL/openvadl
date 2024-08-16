package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM node for bitwise xor.
 */
public class LlvmXorSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmXorSD(NodeList<ExpressionNode> args,
                   Type type) {
    super(BuiltInTable.XOR, args, type);
  }

  @Override
  public String lower() {
    return "xor";
  }
}
