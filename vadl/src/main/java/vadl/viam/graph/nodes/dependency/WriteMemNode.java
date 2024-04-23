package vadl.viam.graph.nodes.dependency;

/**
 * Represents a write to memory.
 *
 * <p>Even though this is a side effect, it is both, a {@link vadl.viam.graph.nodes.DependencyNode}
 * and a {@link vadl.viam.graph.UniqueNode}. This is because of VADL's semantic constraints
 * for memory writes:
 * <li>A location may only be written once per instruction</li>
 * <li>All reads must occur before all writes</li>
 * </p>
 */
public class WriteMemNode extends WriteNode {
  public WriteMemNode(ExpressionNode location, ExpressionNode value) {
    super(location, value);
  }
}
