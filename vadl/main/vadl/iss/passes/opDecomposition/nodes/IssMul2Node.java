package vadl.iss.passes.opDecomposition.nodes;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.TupleType;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The ISS Mul2 node represents a long multiplication that returns a tuple of two integers.
 * The first one holds the lower half of the multiplication, while the second one is the upper
 * half.
 * We need this as long multiplication tends to exceed the maximum supported result size of
 * 64bit. So we have to split it into two smaller results.
 */
public class IssMul2Node extends IssBinaryNode {

  @DataValue
  private IssMulKind kind;


  public IssMul2Node(ExpressionNode arg1, ExpressionNode arg2, IssMulKind kind,
                     TupleType resultType) {
    super(arg1, arg2, resultType);
    this.kind = kind;
  }

  public IssMulKind kind() {
    return kind;
  }

  @Override
  public TupleType type() {
    return (TupleType) super.type();
  }

  @Override
  public ExpressionNode copy() {
    return new IssMul2Node(arg1().copy(), arg2().copy(), kind, type());
  }

  @Override
  public ExpressionNode shallowCopy() {
    return new IssMul2Node(arg1(), arg2(), kind, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // not used
  }


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(kind);
  }
}
