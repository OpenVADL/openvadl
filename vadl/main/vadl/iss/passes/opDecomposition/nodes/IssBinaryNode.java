package vadl.iss.passes.opDecomposition.nodes;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

public abstract class IssBinaryNode extends IssExprNode {

  @Input
  private ExpressionNode arg1;

  @Input
  private ExpressionNode arg2;

  public IssBinaryNode(ExpressionNode arg1, ExpressionNode arg2, Type type) {
    super(type);
    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  public ExpressionNode arg1() {
    return arg1;
  }

  public ExpressionNode arg2() {
    return arg2;
  }

  @Override
  public Type type() {
    return super.type();
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    arg1 = visitor.apply(this, arg1, ExpressionNode.class);
    arg2 = visitor.apply(this, arg2, ExpressionNode.class);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(arg1);
    collection.add(arg2);
  }
}
