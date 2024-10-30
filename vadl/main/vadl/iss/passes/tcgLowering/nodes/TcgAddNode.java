package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_gen_add} TCG instruction in the TCG VIAM lowering.
 */
public class TcgAddNode extends TcgBinaryOpNode {

  public TcgAddNode(TcgV resVar, TcgV arg1, TcgV arg2) {
    super(resVar, arg1, arg2, resVar.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_add";
  }

  @Override
  public Node copy() {
    return new TcgAddNode(res, arg1, arg2);
  }

  @Override
  public Node shallowCopy() {
    return new TcgAddNode(res, arg1, arg2);
  }
}
