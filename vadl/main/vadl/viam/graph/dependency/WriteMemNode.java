package vadl.viam.graph.dependency;

import vadl.viam.graph.UniqueNode;

/**
 * Represents a write to memory.
 *
 * <p>Even though this is a side effect, it is both, a {@link DependencyNode}
 * and a {@link UniqueNode}. This is because of VADL's semantic constraints
 * for memory writes:
 * <li>A location may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteMemNode extends WriteResourceNode {
  public WriteMemNode(ExpressionNode location, ExpressionNode value) {
    super(location, value);
  }
}
