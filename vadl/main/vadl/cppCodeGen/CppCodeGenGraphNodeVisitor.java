package vadl.cppCodeGen;

import vadl.cppCodeGen.passes.type_normalization.UpcastedTypeCastNode;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for nodes of the cpp codegen layer.
 */
public interface CppCodeGenGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link UpcastedTypeCastNode}.
   */
  void visit(UpcastedTypeCastNode upcastedTypeCastNode);
}
