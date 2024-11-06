package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.viam.graph.Node;

public class TcgBr extends TcgOpNode {

  private final TcgLabel label;

  public TcgBr(TcgLabel label) {
    // TODO: This super constructor is useless. We need to create a TcgGenNode super type
    super(TcgV.gen(TcgWidth.i64), TcgWidth.i64);
    this.label = label;
  }

  @Override
  public Node copy() {
    return new TcgBr(label);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(label);
  }
}
