package vadl.gcb.passes.assembly.visitors;

import vadl.gcb.passes.assembly.AssemblyConstant;
import vadl.viam.Assembly;
import vadl.viam.Function;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visits nodes in {@link Function#behavior()} of {@link Assembly}.
 */
public interface AssemblyVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link AssemblyConstant}.
   */
  void visit(AssemblyConstant node);
}
