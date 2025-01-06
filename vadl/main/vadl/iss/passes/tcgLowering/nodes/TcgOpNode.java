package vadl.iss.passes.tcgLowering.nodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents an abstract TCG (Tiny Code Generation) operation node.
 * It extends the DirectionalNode class and contains a result variable and a width specification.
 * This class is designed to be the base class for specific TCG operator nodes.
 */
public abstract class TcgOpNode extends TcgNode {

  @Input
  TcgVRefNode dest;
  @DataValue
  Tcg_32_64 width;

  public TcgOpNode(TcgVRefNode dest, Tcg_32_64 width) {
    this.dest = dest;
    this.width = width;
  }

  public TcgOpNode(TcgVRefNode dest) {
    this.dest = dest;
    this.width = dest.var().width();
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(dest.var().width() == width, "result variable width does not match");
  }

  public Tcg_32_64 width() {
    return width;
  }

  public TcgVRefNode dest() {
    return dest;
  }

  public void setDest(TcgVRefNode res) {
    this.dest = res;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return new HashSet<>();
  }

  @Override
  public @Nullable TcgVRefNode definedVar() {
    return dest;
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
    dest = visitor.apply(this, dest, TcgVRefNode.class);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(dest);
  }
}
