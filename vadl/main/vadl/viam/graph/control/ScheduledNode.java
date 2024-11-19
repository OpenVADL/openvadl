package vadl.viam.graph.control;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.DependencyNode;

public class ScheduledNode extends DirectionalNode {

  @Input
  public DependencyNode node;

  public ScheduledNode(DependencyNode node) {
    this.node = node;
  }

  public DependencyNode node() {
    return node;
  }

  @Override
  public Node copy() {
    return new ScheduledNode(node.copy(DependencyNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new ScheduledNode(node);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Node visitor is not supported
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(node);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    node = visitor.apply(this, node, DependencyNode.class);
  }
}
