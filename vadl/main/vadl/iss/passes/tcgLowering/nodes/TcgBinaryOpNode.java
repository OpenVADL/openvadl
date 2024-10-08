package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;

/**
 * A common superclass that represents a TCG operator with two source variables and one result.
 */
public abstract class TcgBinaryOpNode extends TcgOpNode {

  @DataValue
  TcgV arg1;

  @DataValue
  TcgV arg2;


  /**
   * Constructs a TcgBinaryOpNode with specified result variable, two argument variables,
   * and a specified bit width.
   *
   * @param resultVar the variable that will store the result of the binary operation
   * @param arg1      the first argument variable
   * @param arg2      the second argument variable
   * @param width     the bit width of the operation
   */
  public TcgBinaryOpNode(TcgV resultVar, TcgV arg1, TcgV arg2, TcgWidth width) {
    super(resultVar, width);
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(arg1.width() == width, "argument 1 width does not match");
    ensure(arg2.width() == width, "argument 2 width does not match");
  }

  public TcgV arg1() {
    return arg1;
  }

  public TcgV arg2() {
    return arg2;
  }

  public abstract String tcgFunctionName();

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(arg1);
    collection.add(arg2);
  }
}
