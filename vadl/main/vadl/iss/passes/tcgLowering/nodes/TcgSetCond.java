package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

public class TcgSetCond extends TcgBinaryOpNode {

  @DataValue
  private TcgCondition cond;

  public TcgSetCond(TcgV resultVar, TcgV arg1, TcgV arg2, TcgCondition cond) {
    super(resultVar, arg1, arg2, resultVar.width());
    this.cond = cond;
  }

  public TcgCondition condition() {
    return cond;
  }

  @Override
  public Node copy() {
    return new TcgSetCond(res, arg1, arg2, cond);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_setcond";
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(cond);
  }
}
