package vadl.viam.graph.dependency;

import vadl.viam.graph.UniqueNode;

/**
 * Represents a write to register.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for register writes:
 * <li>A register may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteRegNode extends WriteResourceNode {
  public WriteRegNode(ExpressionNode location, ExpressionNode value) {
    super(location, value);
  }
}
