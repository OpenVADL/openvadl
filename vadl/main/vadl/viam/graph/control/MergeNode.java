package vadl.viam.graph.control;

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * The MergeNode depends on a list of branches that merge control flow again.
 */
public class MergeNode extends AbstractBeginNode {

  @Input
  NodeList<BranchEndNode> branchEnds;

  public MergeNode(NodeList<BranchEndNode> branchEnds, ControlNode next) {
    super(next);
    this.branchEnds = branchEnds;
  }

  public MergeNode(NodeList<BranchEndNode> branchEnds) {
    this.branchEnds = branchEnds;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(branchEnds);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    branchEnds = branchEnds.stream()
        .map(e -> visitor.apply(this, e, BranchEndNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  public String toString() {
    var ids = branchEnds.stream()
        .map(e -> "%s".formatted(e.id))
        .collect(Collectors.joining(", "));
    return "%s(%s)".formatted(super.toString(), ids);
  }

  @Override
  public Node copy() {
    return new MergeNode(
        new NodeList<>(this.branchEnds.stream().map(x -> (BranchEndNode) x.copy()).toList()),
        (ControlNode) next().copy());
  }

  @Override
  public Node shallowCopy() {
    return new MergeNode(branchEnds, (ControlNode) next());
  }
}
