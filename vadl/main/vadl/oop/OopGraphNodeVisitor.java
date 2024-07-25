package vadl.oop;

import vadl.oop.passes.type_normalization.UpcastedTypeCastNode;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for nodes of the oop layer.
 */
public interface OopGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link UpcastedTypeCastNode}.
   */
  void visit(UpcastedTypeCastNode upcastedTypeCastNode);
}
