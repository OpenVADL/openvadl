package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.graph.Node;


/**
 * Represents a node for a move operation in a TCG.
 * It includes the result and argument of the move operation, along with the width specification.
 */
public class TcgMoveNode extends TcgUnaryOpNode {

  public TcgMoveNode(TcgV to, TcgV from) {
    super(to, from);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_mov_" + dest.width();
  }


  @Override
  public Node copy() {
    return new TcgMoveNode(dest, arg);
  }

  @Override
  public Node shallowCopy() {
    return new TcgMoveNode(dest, arg);
  }

}
