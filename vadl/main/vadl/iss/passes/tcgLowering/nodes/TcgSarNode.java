package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * Represents an arithmetic right shift operation in the Tiny Code Generator (TCG).
 * This class extends TcgBinaryImmOpNode to perform an arithmetic right shift operation
 * on a source variable by a specified immediate value.
 */
public class TcgSarNode extends TcgBinaryOpNode {

  public TcgSarNode(TcgVRefNode res, TcgVRefNode arg, TcgVRefNode shiftAmount) {
    super(res, arg, shiftAmount, res.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_sar";
  }

  @Override
  public Node copy() {
    return new TcgSarNode(firstDest().copy(TcgVRefNode.class),
        arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgSarNode(firstDest(), arg1, arg2);
  }
}
