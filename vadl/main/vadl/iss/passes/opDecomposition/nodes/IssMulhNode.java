package vadl.iss.passes.opDecomposition.nodes;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents an ISS intermediate multiplication node that returns only
 * the upper half of the multiplication result.
 */
public class IssMulhNode extends IssBinaryNode {

  @DataValue
  private IssMulKind kind;

  public IssMulhNode(ExpressionNode arg1, ExpressionNode arg2, IssMulKind kind, Type type) {
    super(arg1, arg2, type);
    this.kind = kind;
  }

  public IssMulKind kind() {
    return kind;
  }

  @Override
  public ExpressionNode copy() {
    return new IssMulhNode(arg1().copy(), arg2().copy(), kind, type());
  }

  @Override
  public Node shallowCopy() {
    return new IssMulhNode(arg1(), arg2(), kind, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(kind);
  }
}
