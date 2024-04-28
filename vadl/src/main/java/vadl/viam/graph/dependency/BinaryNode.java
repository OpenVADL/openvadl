package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

public abstract class BinaryNode extends ExpressionNode {

  @Input
  protected ExpressionNode x;
  @Input
  protected ExpressionNode y;

  public BinaryNode(ExpressionNode x, ExpressionNode y) {
    this.x = x;
    this.y = y;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(x);
    collection.add(y);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    x = visitor.apply(this, x, ExpressionNode.class);
    y = visitor.apply(this, y, ExpressionNode.class);
  }
}
