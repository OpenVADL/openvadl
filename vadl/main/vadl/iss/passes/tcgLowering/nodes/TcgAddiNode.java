package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents the {@code tcg_gen_addi} TCG instruction in the TCG VIAM lowering.
 */
public class TcgAddiNode extends TcgBinaryImmOpNode {


  public TcgAddiNode(TcgV res, TcgV arg1, ExpressionNode arg2,
                     TcgWidth width) {
    super(res, arg1, arg2, width);
  }

  @Override
  public Node copy() {
    return new TcgAddiNode(
        res, arg1, arg2.copy(ExpressionNode.class), width);
  }

  @Override
  public Node shallowCopy() {
    return new TcgAddiNode(res, arg1, arg2, width);
  }

}
