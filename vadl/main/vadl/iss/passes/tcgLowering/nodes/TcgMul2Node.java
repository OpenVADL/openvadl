package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.opDecomposition.nodes.IssMulKind;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.NodeList;

public class TcgMul2Node extends TcgBinaryOpNode {

  @DataValue
  private IssMulKind kind;

  public TcgMul2Node(IssMulKind kind, TcgVRefNode lowerHalfDest, TcgVRefNode upperHalfDest,
                     TcgVRefNode arg1, TcgVRefNode arg2) {
    super(new NodeList<TcgVRefNode>(lowerHalfDest, upperHalfDest), arg1, arg2,
        lowerHalfDest.width());
    this.kind = kind;
  }

  public IssMulKind kind() {
    return kind;
  }

  public TcgVRefNode lowerHalfDest() {
    return destinations().get(0);
  }

  public TcgVRefNode upperHalfDest() {
    return destinations().get(1);
  }

  @Override
  public String tcgFunctionName() {
    var kind = kindToString();
    return "tcg_gen_mul" + kind + "2";
  }

  private String kindToString() {
    return switch (kind) {
      case SIGNED_SIGNED -> "s";
      case UNSIGNED_UNSIGNED -> "u";
      case SIGNED_UNSIGNED -> "su";
    };
  }

  @Override
  public TcgMul2Node copy() {
    return new TcgMul2Node(kind, lowerHalfDest().copy(), upperHalfDest().copy(),
        arg1.copy(), arg2.copy());
  }

  @Override
  public TcgMul2Node shallowCopy() {
    return new TcgMul2Node(kind, lowerHalfDest(), upperHalfDest(), arg1, arg2);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(kind);
  }
}
