package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;


/**
 * Represents a node for a move operation in a TCG.
 * It includes the result and argument of the move operation, along with the width specification.
 */
public class TcgMoveNode extends TcgOpNode {
  @DataValue
  TcgV arg1;

  public TcgMoveNode(TcgV res, TcgV arg1) {
    super(res, res.width());
    this.arg1 = arg1;
  }

  public TcgV arg1() {
    return arg1;
  }

  @Override
  public Node copy() {
    return new TcgMoveNode(res, arg1);
  }

  @Override
  public Node shallowCopy() {
    return new TcgMoveNode(res, arg1);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(arg1);
  }
}
