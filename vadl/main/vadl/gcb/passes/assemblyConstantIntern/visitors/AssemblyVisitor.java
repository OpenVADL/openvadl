package vadl.gcb.passes.assemblyConstantIntern.visitors;

import vadl.viam.Assembly;
import vadl.viam.Function;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visits nodes in {@link Function#behavior()} of {@link Assembly}.
 */
public interface AssemblyVisitor extends GraphNodeVisitor {
}
