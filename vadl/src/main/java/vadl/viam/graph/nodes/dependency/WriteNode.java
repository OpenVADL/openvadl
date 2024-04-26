package vadl.viam.graph.nodes.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.nodes.DependencyNode;

/**
 * Represents a write operation to some location that produces a side
 * effect.
 */
public abstract class WriteNode extends SideEffectNode {

  @Input
  protected DependencyNode location;

  @Input
  protected ExpressionNode value;

  public WriteNode(DependencyNode location, ExpressionNode value) {
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
    super.applyOnInputs(visitor);
    location = visitor.apply(this, location, ExpressionNode.class);
    value = visitor.apply(this, value, ExpressionNode.class);
  }
}
