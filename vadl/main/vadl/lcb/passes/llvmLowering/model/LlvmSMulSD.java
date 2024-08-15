package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * LLVM node for signed multiplication.
 */
public class LlvmSMulSD extends BuiltInCall implements LlvmNodeLowerable {
  public LlvmSMulSD(NodeList<ExpressionNode> args,
                    Type type) {
    super(BuiltInTable.SMULL, args, type);
  }

  @Override
  public String lower() {
    return "smul_lohi";
  }
}
