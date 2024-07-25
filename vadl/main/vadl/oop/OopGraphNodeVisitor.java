package vadl.oop;

import vadl.oop.passes.type_normalization.UpcastedTypeCastNode;
import vadl.viam.graph.GraphNodeVisitor;

public interface OopGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link UpcastedTypeCastNode}.
   */
  void visit(UpcastedTypeCastNode upcastedTypeCastNode);
}
