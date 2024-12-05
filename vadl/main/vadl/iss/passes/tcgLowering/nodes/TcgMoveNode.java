package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.graph.Node;


/**
 * Represents a node for a move operation in a TCG.
 * It includes the result and argument of the move operation, along with the width specification.
 */
public class TcgMoveNode extends TcgUnaryOpNode {

  public TcgMoveNode(TcgVRefNode to, TcgVRefNode from) {
    super(to, from);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_mov_" + dest.width();
  }


  @Override
  public Node copy() {
    return new TcgMoveNode(dest.copy(TcgVRefNode.class), arg.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgMoveNode(dest, arg);
  }

}
