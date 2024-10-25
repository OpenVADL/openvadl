package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;

public abstract class TcgUnaryOpNode extends TcgOpNode {

  @DataValue
  TcgV arg;

  public TcgUnaryOpNode(TcgV res, TcgV arg) {
    super(res, res.width());
    this.arg = arg;
  }

  public TcgV arg() {
    return arg;
  }

  public abstract String tcgFunctionName();

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(arg);
  }
}
