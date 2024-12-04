package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.viam.graph.Node;

public class TcgMulNode extends TcgBinaryOpNode {

  public TcgMulNode(TcgVRefNode resultVar, TcgVRefNode arg1, TcgVRefNode arg2) {
    super(resultVar, arg1, arg2);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_add";
  }

  @Override
  public Node copy() {
    return new TcgMulNode(dest.copy(),
        arg1.copy(),
        arg2.copy()
    );
  }

  @Override
  public Node shallowCopy() {
    return new TcgMulNode(dest, arg1, arg2);
  }
}
