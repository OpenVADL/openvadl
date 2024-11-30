package vadl.iss.passes.tcgLowering.nodes;

import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.viam.graph.Node;

/**
 * Used to define the label position in the emitted TCG code.
 * When branching to the label, execution continues at the position of this label set operation.
 */
public class TcgSetLabel extends TcgLabelNode {


  /**
   * Constructs a new {@code TcgSetLabel} with the specified {@link TcgLabel}.
   *
   * @param label the label to set at the position of this label set operation
   */
  public TcgSetLabel(TcgLabel label) {
    super(label);
  }

  @Override
  public Node copy() {
    return new TcgSetLabel(label());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "gen_set_label(" + label().varName() + ");";
  }
}
