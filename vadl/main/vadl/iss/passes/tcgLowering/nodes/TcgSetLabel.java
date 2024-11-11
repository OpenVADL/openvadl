package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.viam.graph.Node;

/**
 * Used to define the label position in the emitted TCG code.
 * When branching to the label, execution continues at the position of this label set operation.
 */
public class TcgSetLabel extends TcgOpNode {

  private final TcgLabel label;

  /**
   * Constructs a new {@code TcgSetLabel} with the specified {@link TcgLabel}.
   *
   * @param label the label to set at the position of this label set operation
   */
  public TcgSetLabel(TcgLabel label) {
    // TODO: This super constructor is useless. We need to create a TcgGenNode super type
    super(TcgV.gen(TcgWidth.i64), TcgWidth.i64);
    this.label = label;
  }

  public TcgLabel label() {
    return label;
  }

  @Override
  public Node copy() {
    return new TcgSetLabel(label);
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
