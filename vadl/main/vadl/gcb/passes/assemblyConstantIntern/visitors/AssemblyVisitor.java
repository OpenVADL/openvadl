package vadl.gcb.passes.assemblyConstantIntern.visitors;

import vadl.gcb.passes.assemblyConstantIntern.AssemblyConstant;
import vadl.viam.Assembly;
import vadl.viam.Function;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Visits nodes in {@link Function#behavior()} of {@link Assembly}.
 */
public interface AssemblyVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link AssemblyConstant}.
   */
  void visit(AssemblyConstant node);
}
