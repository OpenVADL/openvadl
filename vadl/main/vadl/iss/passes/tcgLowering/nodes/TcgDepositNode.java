package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Deposit t2 as a bitfield into t1, placing the result in dest().
 *
 * <p>The bitfield is described by pos/len, which are immediate values:
 * <ul>
 * <li>
 *   len - the length of the bitfield
 * </li>
 * <li>
 * pos - the position of the first bit, counting from the LSB
 * </li>
 * </ul>
 * For example, {@code deposit_i32 dest, t1, t2, 8, 4} indicates a 4-bit field at bit 8.
 * This operation would be equivalent to
 * {@code dest = (t1 & ~0x0f00) | ((t2 << 8) & 0x0f00)}
 */
public class TcgDepositNode extends TcgBinaryOpNode {

  @DataValue
  private int pos;

  @DataValue
  private int len;

  /**
   * Constructs a TCG deposit node.
   */
  public TcgDepositNode(TcgVRefNode dest,
                        TcgVRefNode t1, TcgVRefNode t2, int pos, int len) {
    super(dest, t1, t2);
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
    return "tcg_gen_deposit_" + width();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "(" + firstDest().varName() + ", " + arg1.varName() + ", "
        + arg2.varName() + ", " + pos + ", " + len + ");";
  }

  @Override
  public TcgDepositNode copy() {
    return new TcgDepositNode(firstDest().copy(), arg1.copy(), arg2.copy(), pos, len);
  }

  @Override
  public Node shallowCopy() {
    return new TcgDepositNode(firstDest(), arg1, arg2, pos, len);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(pos);
    collection.add(len);
  }
}
