package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.javaannotations.viam.DataValue;

public abstract class TcgLabelNode extends TcgNode {

  @DataValue
  private TcgLabel label;

  protected TcgLabelNode(TcgLabel label) {
    this.label = label;
  }

  public TcgLabel label() {
    return label;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(label);
  }
}
