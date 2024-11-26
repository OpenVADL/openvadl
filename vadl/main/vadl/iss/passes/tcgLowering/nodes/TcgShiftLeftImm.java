package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a left shift operation in the Tiny Code Generator (TCG).
 * This class extends TcgBinaryImmOpNode to perform a left shift operation
 * on a source variable by a specified immediate value.
 */
public class TcgShiftLeftImm extends TcgBinaryImmOpNode {

  public TcgShiftLeftImm(TcgV res, TcgV arg, ExpressionNode shiftAmount) {
    super(res, arg, shiftAmount, res.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_shli";
  }

  @Override
  public Node copy() {
    return new TcgShiftLeftImm(dest, arg1, arg2.copy(ExpressionNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgShiftLeftImm(dest, arg1, arg2);
  }
}
