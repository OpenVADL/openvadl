package vadl.viam.graph.nodes.dependency;

/**
 * The ReadMemNode class is a concrete class that extends ReadNode.
 * It represents a node that reads a value from a memory location.
 */
public class ReadMemNode extends ReadNode {
  public ReadMemNode(ExpressionNode location) {
    super(location);
  }
}
