package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * Represents a left shift operation in the Tiny Code Generator (TCG).
 * This class extends TcgBinaryImmOpNode to perform a left shift operation
 * on a source variable by a specified immediate value.
 */
public class TcgShlNode extends TcgBinaryOpNode {

  public TcgShlNode(TcgVRefNode res, TcgVRefNode arg, TcgVRefNode shiftAmount) {
    super(res, arg, shiftAmount, res.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_shl";
  }

  @Override
  public Node copy() {
    return new TcgShlNode(dest.copy(TcgVRefNode.class),
        arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgShlNode(dest, arg1, arg2);
  }
}
