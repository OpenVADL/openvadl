package vadl.lcb;

import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * Visitor for nodes of the lcb layer.
 */
public interface LcbGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Default method for this visitor. Note that this is a fallback
   * and must not be reached.
   */
  default void visit(Node node) {
  }
}
