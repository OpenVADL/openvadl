package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Class representing a truncation operation in the TCG (Tiny Code Generator).
 * This node performs a bit-width truncation on a single operand.
 * Extends from TcgUnaryOpNode to utilize unary operation functionality.
 */
public class TcgTruncateNode extends TcgUnaryOpNode {

  @DataValue
  int bitWidth;

  public TcgTruncateNode(TcgV res, TcgV arg, int bitWidth) {
    super(res, arg);
    this.bitWidth = bitWidth;
  }

  public int bitWidth() {
    return bitWidth;
  }

  @Override
  public String tcgFunctionName() {
    return "gen_trunc";
  }

  @Override
  public Node copy() {
    return new TcgTruncateNode(res, arg, bitWidth);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(bitWidth);
  }
}
