package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

public class TcgShiftLeft extends TcgBinaryImmOpNode {

  public TcgShiftLeft(TcgV res, TcgV arg, ExpressionNode shiftAmount) {
    super(res, arg, shiftAmount, res.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_shli";
  }

  @Override
  public Node copy() {
    return new TcgShiftLeft(res, arg1, arg2.copy(ExpressionNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgShiftLeft(res, arg1, arg2);
  }
}
