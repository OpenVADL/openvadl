package vadl.viam.graph.dependency;

/**
 * The ReadRegNode class is a subclass of ReadNode that represents
 * a node that reads a value from a register location.
 */
public class ReadRegNode extends ReadNode {
  public ReadRegNode(ExpressionNode location) {
    super(location);
  }
}
