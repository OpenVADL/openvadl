package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgCondition;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents the TCG setcond operation that 1 or 0 to dest depending on the
 * comparison of its operands.
 * The cond property defines what comparison operator to use.
 */
public class TcgSetCond extends TcgBinaryOpNode {

  @DataValue
  private TcgCondition cond;

  /**
   * This constructor initializes a TcgSetCond object, representing a setcond operation in TCG.
   *
   * @param resultVar the variable that will store the result of the conditional set operation
   * @param arg1      the first argument variable for the comparison
   * @param arg2      the second argument variable for the comparison
   * @param cond      the condition to be evaluated (e.g., EQ, NE, LT, etc.), determining
   *                  the result of the comparison
   */
  public TcgSetCond(TcgV resultVar, TcgV arg1, TcgV arg2, TcgCondition cond) {
    super(resultVar, arg1, arg2, resultVar.width());
    this.cond = cond;
  }

  public TcgCondition condition() {
    return cond;
  }

  @Override
  public Node copy() {
    return new TcgSetCond(dest, arg1, arg2, cond);
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
