package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_gen_and} TCG instruction in the TCG VIAM lowering.
 */
public class TcgAndNode extends TcgBinaryOpNode {

  public TcgAndNode(TcgVRefNode resVar, TcgVRefNode arg1, TcgVRefNode arg2) {
    super(resVar, arg1, arg2, resVar.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_and";
  }

  @Override
  public Node copy() {
    return new TcgAndNode(dest.copy(TcgVRefNode.class), arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgAndNode(dest, arg1, arg2);
  }
}
