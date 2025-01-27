package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * @deprecated This is currently deprecated as there is no tcg_gen_mulsh / muluh. We
 *     might want to contribute to QEMU to add those TCG ops.
 *     For now, we use the mulu2 and muls2 operations instead.
 */
@Deprecated
public class TcgMulhNode extends TcgBinaryOpNode {

  @DataValue
  boolean signed;

  public TcgMulhNode(boolean signed, TcgVRefNode dest, TcgVRefNode arg1,
                     TcgVRefNode arg2) {
    super(dest, arg1, arg2);
    this.signed = signed;
  }

  public boolean isSigned() {
    return signed;
  }

  @Override
  public String tcgFunctionName() {
    var sign = signed ? "s" : "u";
    return "tcg_gen_mul" + sign + "h";
  }

  @Override
  public Node copy() {
    return new TcgMulhNode(signed, firstDest().copy(), arg1().copy(), arg2().copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgMulhNode(signed, firstDest(), arg1(), arg2());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(signed);
  }
}
