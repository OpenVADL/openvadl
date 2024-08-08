package vadl.cpp_codegen;

import vadl.cpp_codegen.passes.type_normalization.UpcastedTypeCastNode;
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
