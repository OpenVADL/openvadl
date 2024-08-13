package vadl.cppCodeGen;

import vadl.cppCodeGen.passes.type_normalization.CppSignExtendNode;
import vadl.cppCodeGen.passes.type_normalization.CppTruncateNode;
import vadl.cppCodeGen.passes.type_normalization.CppZeroExtendNode;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for nodes of the cpp codegen layer.
 */
public interface CppCodeGenGraphNodeVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link CppSignExtendNode}.
   */
  void visit(CppSignExtendNode node);

  /**
   * Visit {@link CppZeroExtendNode}.
   */
  void visit(CppZeroExtendNode node);

  /**
   * Visit {@link CppTruncateNode}.
   */
  void visit(CppTruncateNode node);
}
