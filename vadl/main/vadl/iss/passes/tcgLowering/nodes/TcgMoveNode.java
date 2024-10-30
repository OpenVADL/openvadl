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
public class TcgMoveNode extends TcgUnaryOpNode {
  
  public TcgMoveNode(TcgV to, TcgV from) {
    super(to, from);
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_mov";
  }


  @Override
  public Node copy() {
    return new TcgMoveNode(res, arg);
  }

  @Override
  public Node shallowCopy() {
    return new TcgMoveNode(res, arg);
  }

}
