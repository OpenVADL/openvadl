package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Extract a bitfield from t1, placing the result in dest().
 *
 * <p>The bitfield is described by pos/len, which are immediate values, as above for deposit.
 * For extract_*, the result will be extended to the left with zeros; for sextract_*,
 * the result will be extended to the left with copies of the bitfield sign bit at pos + len - 1.
 *
 * <p>For example, “sextract_i32 dest, t1, 8, 4” indicates a 4-bit field at bit 8.
 * This operation would be equivalent to
 * {@code dest = (t1 << 20) >> 28} (using an arithmetic right shift).
 */
public class TcgExtractNode extends TcgUnaryOpNode {

  @DataValue
  private int pos;

  @DataValue
  private int len;

  public TcgExtractNode(TcgVRefNode dest,
                        TcgVRefNode t1, int pos, int len) {
    super(dest, t1);
    this.pos = pos;
    this.len = len;
  }

  public int pos() {
    return pos;
  }

  public int len() {
    return len;
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_extract_" + width();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg.varName() + ", " + pos +
        ", "
        + len + ");";
  }

  @Override
  public TcgExtractNode copy() {
    return new TcgExtractNode(firstDest().copy(), arg.copy(), pos, len);
  }

  @Override
  public Node shallowCopy() {
    return new TcgExtractNode(firstDest(), arg, pos, len);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(pos);
    collection.add(len);
  }
}
