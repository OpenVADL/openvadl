package vadl.viam.graph.nodes.dependency;

import vadl.viam.graph.nodes.DependencyNode;

/**
 * Represents a write to register.
 *
 * <p>Even though this is a side effect, it is both, a {@link vadl.viam.graph.nodes.DependencyNode}
 * and a {@link vadl.viam.graph.UniqueNode}. This is because of VADL's semantic constraints
 * for register writes:
 * <li>A register may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteRegNode extends WriteNode {
  public WriteRegNode(DependencyNode location, ExpressionNode value) {
    super(location, value);
  }
}
