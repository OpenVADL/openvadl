package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;


/**
 * Represents the {@code tcg_gen_mul} TCG instruction in the TCG VIAM lowering.
 */
public class TcgMulNode extends TcgBinaryOpNode {

  public TcgMulNode(TcgVRefNode resultVar, TcgVRefNode arg1, TcgVRefNode arg2) {
    super(resultVar, arg1, arg2);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_mul";
  }

  @Override
  public Node copy() {
    return new TcgMulNode(firstDest().copy(),
        arg1.copy(),
        arg2.copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new TcgMulNode(firstDest(), arg1, arg2);
  }
}
