package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM node for subtraction.
 */
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
