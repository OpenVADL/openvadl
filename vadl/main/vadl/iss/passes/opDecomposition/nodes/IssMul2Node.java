package vadl.iss.passes.opDecomposition.nodes;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.TupleType;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.dependency.ExpressionNode;

public class IssMul2Node extends IssBinaryNode {

  @DataValue
  private IssMulKind kind;


  public IssMul2Node(ExpressionNode arg1, ExpressionNode arg2, IssMulKind kind,
                     TupleType resultType) {
    // TODO: Don't hardcode this
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
