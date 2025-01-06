package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * An abstract base class for TCG nodes that are associated with a {@link TcgV} variable.
 * This class provides common functionality for nodes that work
 * with TCG variables in the TCG lowering process.
 */
public abstract class TcgVarNode extends TcgNode {

  /**
   * The TCG variable associated with this node.
   */
  @Input
  private TcgVRefNode variable;

  public TcgVarNode(TcgVRefNode variable) {
    this.variable = variable;
  }

  /**
   * Returns the {@link TcgV} variable associated with this node.
   *
   * @return The TCG variable of this node.
   */
  public TcgVRefNode variable() {
    return variable;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return Set.of();
  }

  @Override
  public @Nullable TcgVRefNode definedVar() {
    return null;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(variable);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    variable = visitor.apply(this, variable, TcgVRefNode.class);
  }
}