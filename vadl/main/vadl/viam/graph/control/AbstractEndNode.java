package vadl.viam.graph.control;

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The AbstractEndNode represents the end of a control flow.
 * This may be a subflow (e.g. an if branch) or the main flow.
 *
 * <p>It holds a list of side effects that are required
 * to be executed before the end of this branch.</p>
 */
public abstract class AbstractEndNode extends ControlNode {
  @Input
  private NodeList<SideEffectNode> sideEffects;

  public AbstractEndNode(NodeList<SideEffectNode> sideEffects) {
    this.sideEffects = sideEffects;
  }

  public NodeList<SideEffectNode> sideEffects() {
    return sideEffects;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(sideEffects);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    sideEffects = sideEffects.stream()
        .map(e -> visitor.apply(this, e, SideEffectNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  /**
   * Adds a side effect to the node.
   */
  public void addSideEffect(SideEffectNode sideEffectNode) {
    this.sideEffects.add(sideEffectNode);
    updateUsageOf(null, sideEffectNode);
  }
}
