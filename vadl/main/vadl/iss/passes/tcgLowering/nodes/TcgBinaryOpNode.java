package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;

public abstract class TcgBinaryOpNode extends TcgOpNode {

  @DataValue
  TcgV arg1;

  @DataValue
  TcgV arg2;

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
