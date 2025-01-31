package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
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
    return "tcg_gen_mov_" + firstDest().width();
  }


  @Override
  public Node copy() {
    return new TcgMoveNode(firstDest().copy(TcgVRefNode.class), arg.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgMoveNode(firstDest(), arg);
  }

}
