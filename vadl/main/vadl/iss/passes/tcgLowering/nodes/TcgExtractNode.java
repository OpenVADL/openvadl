package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
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
  private final int offset;

  @DataValue
  private final int len;

  @DataValue
  private final TcgExtend extendMode;

  /**
   * Construct a TCG extract node.
   *
   * @param dest   of result
   * @param t1     source variable
   * @param offset offset (from lsb) where to start extraction
   * @param len    of extraction (from lsb)
   */
  public TcgExtractNode(TcgVRefNode dest,
                        TcgVRefNode t1, int offset, int len, TcgExtend extendMode) {
    super(dest, t1);
    this.offset = offset;
    this.len = len;
    this.extendMode = extendMode;
  }

  public int pos() {
    return offset;
  }

  public int len() {
    return len;
  }

  public TcgExtend signed() {
    return extendMode;
  }

  @Override
  public String tcgFunctionName() {
    var sign = extendMode == TcgExtend.SIGN ? "s" : "";
    return "tcg_gen_" + sign + "extract_" + width();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg.varName() + ", " + offset
        + ", "
        + len + ");";
  }

  @Override
  public TcgExtractNode copy() {
    return new TcgExtractNode(firstDest().copy(), arg.copy(), offset, len, extendMode);
  }

  @Override
  public Node shallowCopy() {
    return new TcgExtractNode(firstDest(), arg, offset, len, extendMode);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(offset);
    collection.add(len);
    collection.add(extendMode);
  }
}
