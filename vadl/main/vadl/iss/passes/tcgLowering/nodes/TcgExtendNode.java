package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.iss.passes.tcgLowering.Tcg_8_16_32;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represents a unary operation node
 * that extends a value from a smaller bit-width to a larger bit-width.
 * This class handles both sign- and zero-extension operations.
 */
public class TcgExtendNode extends TcgUnaryOpNode {

  @DataValue
  Tcg_8_16_32 fromSize;

  @DataValue
  TcgExtend extend;

  /**
   * Initializes a TcgExtendNode, representing an operation that extends a value from a
   * smaller bit-width to a larger bit-width, handling both sign- and zero-extension.
   *
   * @param fromSize The original size (bit-width) of the value.
   * @param extend   The type of extension (sign or zero) to be applied.
   * @param res      The result variable of the operation.
   * @param arg      The argument variable to be extended.
   */
  public TcgExtendNode(Tcg_8_16_32 fromSize, TcgExtend extend, TcgV res, TcgV arg) {
    super(res, arg);
    this.fromSize = fromSize;
    this.extend = extend;
  }

  @Override
  public Node copy() {
    return new TcgExtendNode(fromSize, extend, res, arg);
  }

  @Override
  public Node shallowCopy() {
    return new TcgExtendNode(fromSize, extend, res, arg);
  }

  @Override
  public String tcgFunctionName() {
    var postfix = extend == TcgExtend.SIGN ? "s" : "u";
    return "tcg_gen_ext" + fromSize.width + postfix + "_i" + width.width;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(fromSize);
    collection.add(extend);
  }
}
