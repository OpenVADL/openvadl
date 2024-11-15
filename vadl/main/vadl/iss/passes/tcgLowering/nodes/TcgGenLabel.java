package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents a TCG (Tiny Code Generation) label generation node.
 */
public class TcgGenLabel extends TcgOpNode {

  @DataValue
  private final TcgLabel label;

  /**
   * Creates a TcgGenLabel instance which represents a TCG label generation node.
   *
   * @param label The TCG label associated with this node.
   */
  public TcgGenLabel(TcgLabel label) {
    // TODO: This super constructor is useless. We need to create a TcgGenNode super type
    super(TcgV.gen(TcgWidth.i64), TcgWidth.i64);
    this.label = label;
  }

  public TcgLabel label() {
    return label;
  }

  @Override
  public Node copy() {
    return new TcgGenLabel(label);
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
