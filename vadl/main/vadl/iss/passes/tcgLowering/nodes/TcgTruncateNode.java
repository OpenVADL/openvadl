package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
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

  public TcgTruncateNode(TcgVRefNode res, TcgVRefNode arg, int bitWidth) {
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
  public String cCode(Function<Node, String> nodeToCCode) {
    return tcgFunctionName() + "("
        + firstDest().varName()
        + ", " + arg.varName()
        + ", " + bitWidth
        + ");";
  }

  @Override
  public Node copy() {
    return new TcgTruncateNode(firstDest().copy(TcgVRefNode.class), arg.copy(TcgVRefNode.class),
        bitWidth);
  }

  @Override
  public Node shallowCopy() {
    return new TcgTruncateNode(firstDest(), arg, bitWidth);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(bitWidth);
  }
}
