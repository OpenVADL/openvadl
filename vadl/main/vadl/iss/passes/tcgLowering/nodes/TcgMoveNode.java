package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.viam.graph.Node;

public class TcgMoveNode extends TcgOpNode {
  TcgV arg1;

  public TcgMoveNode(TcgV res, TcgV arg1,
                     TcgWidth width) {
    super(res, width);
    this.arg1 = arg1;
  }

  public TcgV arg1() {
    return arg1;
  }

  @Override
  public Node copy() {
    return new TcgMoveNode(res, arg1, width);
  }

  @Override
  public Node shallowCopy() {
    return new TcgMoveNode(res, arg1, width);
  }
}
