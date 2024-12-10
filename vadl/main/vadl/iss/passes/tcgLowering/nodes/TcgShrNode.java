package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * Represents a right shift operation in the Tiny Code Generator (TCG).
 * This class extends TcgBinaryImmOpNode to perform a right shift operation
 * on a source variable by a specified immediate value.
 */
public class TcgShrNode extends TcgBinaryOpNode {

  public TcgShrNode(TcgVRefNode res, TcgVRefNode arg, TcgVRefNode shiftAmount) {
    super(res, arg, shiftAmount, res.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_shr";
  }

  @Override
  public Node copy() {
    return new TcgShrNode(dest.copy(TcgVRefNode.class),
        arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgShrNode(dest, arg1, arg2);
  }
}
