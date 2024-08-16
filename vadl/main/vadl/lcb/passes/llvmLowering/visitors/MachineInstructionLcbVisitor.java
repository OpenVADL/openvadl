package vadl.lcb.passes.llvmLowering.visitors;

import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.lcb.visitors.LcbGraphNodeVisitor;
import vadl.viam.graph.Graph;

/**
 * Visitor for machine instruction's {@link Graph}.
 */
public interface MachineInstructionLcbVisitor extends LcbGraphNodeVisitor {
  /**
   * Visit {@link MachineInstructionNode}.
   */
  void visit(MachineInstructionNode machineInstructionNode);
}
