package vadl.iss.passes.tcgLowering.nodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents an abstract TCG (Tiny Code Generation) operation node.
 * It extends the DirectionalNode class and contains a result variable and a width specification.
 * This class is designed to be the base class for specific TCG operator nodes.
 */
public abstract class TcgOpNode extends TcgNode {

  @Input
  private NodeList<TcgVRefNode> destinations;
  @DataValue
  private Tcg_32_64 width;

  public TcgOpNode(TcgVRefNode singleDest, Tcg_32_64 width) {
    this.destinations = new NodeList<>(singleDest);
    this.width = width;
  }

  public TcgOpNode(NodeList<TcgVRefNode> destinations, Tcg_32_64 width) {
    this.destinations = destinations;
    this.width = width;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    for (var d : destinations) {
      ensure(d.var().width() == width, "result variable width does not match");
    }
  }

  public Tcg_32_64 width() {
    return width;
  }

  public TcgVRefNode firstDest() {
    return destinations.get(0);
  }

  public List<TcgVRefNode> destinations() {
    return destinations;
  }

  public void setDest(NodeList<TcgVRefNode> res) {
    this.destinations = res;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return new HashSet<>();
  }

  @Override
  public List<TcgVRefNode> definedVars() {
    return destinations;
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    // Do not visit by graph visitor
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(width);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    destinations = destinations.stream().map((e) -> visitor.apply(this, e, TcgVRefNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(destinations);
  }
}
