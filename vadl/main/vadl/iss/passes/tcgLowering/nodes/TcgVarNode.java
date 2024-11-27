package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;

/**
 * An abstract base class for TCG nodes that are associated with a {@link TcgV} variable.
 * This class provides common functionality for nodes that work
 * with TCG variables in the TCG lowering process.
 */
public abstract class TcgVarNode extends TcgNode {

  /**
   * The TCG variable associated with this node.
   */
  @DataValue
  private TcgV variable;

  public TcgVarNode(TcgV variable) {
    this.variable = variable;
  }

  /**
   * Returns the {@link TcgV} variable associated with this node.
   *
   * @return The TCG variable of this node.
   */
  public TcgV variable() {
    return variable;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(variable);
  }
}