package vadl.iss.passes.tcgLowering.nodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;

/**
 * An abstract base class for TCG nodes that are associated with a {@link TcgLabel}.
 * This class provides common functionality for nodes that work with
 * labels in the TCG lowering process.
 */
public abstract class TcgLabelNode extends TcgNode {

  /**
   * The label associated with this TCG node.
   */
  @DataValue
  private TcgLabel label;

  /**
   * Constructs a new {@code TcgLabelNode} with the specified label.
   *
   * @param label The {@link TcgLabel} associated with this node.
   */
  protected TcgLabelNode(TcgLabel label) {
    this.label = label;
  }

  /**
   * Returns the {@link TcgLabel} associated with this node.
   *
   * @return The label of this node.
   */
  public TcgLabel label() {
    return label;
  }

  @Override
  public Set<TcgVRefNode> usedVars() {
    return new HashSet<>();
  }

  @Override
  public @Nullable TcgVRefNode definedVar() {
    return null;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(label);
  }
}