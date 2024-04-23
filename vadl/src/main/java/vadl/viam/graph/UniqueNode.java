package vadl.viam.graph;

/**
 * This is a marker class that marks a node to be unique in the graph
 * such that there is no other unique node with equal inputs and
 * equal data properties according to {@link Node#equalData(Node)}.
 */
public interface UniqueNode {

  default Node asNode() {
    return (Node) this;
  }
}
