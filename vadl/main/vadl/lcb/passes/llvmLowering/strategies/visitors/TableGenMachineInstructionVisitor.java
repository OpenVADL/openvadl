package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.viam.graph.Graph;

/**
 * Visitor for machine instruction's {@link Graph}.
 */
public interface TableGenMachineInstructionVisitor extends TableGenNodeVisitor {
  /**
   * Visit {@link MachineInstructionNode}.
   */
  void visit(MachineInstructionNode machineInstructionNode);
}
