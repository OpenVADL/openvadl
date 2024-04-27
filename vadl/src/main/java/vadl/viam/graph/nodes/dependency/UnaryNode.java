package vadl.viam.graph.nodes.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

public abstract class UnaryNode extends ExpressionNode {

  @Input
  protected ExpressionNode value;

  public UnaryNode(ExpressionNode value) {
    this.value = value;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }
}
