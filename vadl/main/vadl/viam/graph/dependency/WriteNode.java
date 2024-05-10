package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a write operation to some location that produces a side
 * effect.
 */
public abstract class WriteNode extends SideEffectNode {

  @Input
  protected ExpressionNode location;

  @Input
  protected ExpressionNode value;

  public WriteNode(ExpressionNode location, ExpressionNode value) {
    this.location = location;
    this.value = value;
  }


  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(location);
    collection.add(value);
  }


  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    location = visitor.apply(this, location, ExpressionNode.class);
    value = visitor.apply(this, value, ExpressionNode.class);
  }
}
