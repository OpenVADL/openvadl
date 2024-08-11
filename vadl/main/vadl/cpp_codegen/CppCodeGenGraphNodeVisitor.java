package vadl.cpp_codegen;

import vadl.viam.graph.GraphNodeVisitor;
import vadl.cpp_codegen.passes.type_normalization.UpcastedTypeCastNode;

/**
 * Visitor for nodes of the cpp codegen layer.
 */
public interface CppCodeGenGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link UpcastedTypeCastNode}.
   */
  void visit(UpcastedTypeCastNode upcastedTypeCastNode);
}
