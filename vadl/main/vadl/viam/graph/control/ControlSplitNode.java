package vadl.viam.graph.control;

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Represents a control flow node that causes diverging execution.
 */
public abstract class ControlSplitNode extends ControlNode {

  @Successor
  private NodeList<BeginNode> branches;

  ControlSplitNode(NodeList<BeginNode> branches) {
    this.branches = branches;
  }

  public NodeList<BeginNode> branches() {
    return branches;
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    collection.addAll(branches);
  }

  @Override
  protected void applyOnSuccessorsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnSuccessorsUnsafe(visitor);
    branches = branches.stream().map(e ->
            visitor.apply(this, e, BeginNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }
}
