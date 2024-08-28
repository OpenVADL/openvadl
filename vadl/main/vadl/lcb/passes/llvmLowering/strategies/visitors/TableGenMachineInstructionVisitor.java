package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionParameterNode;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for machine instruction's {@link Graph}.
 */
public interface TableGenMachineInstructionVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link MachineInstructionNode}.
   */
  void visit(MachineInstructionNode machineInstructionNode);

  /**
   * Visit {@link MachineInstructionParameterNode}.
   */
  void visit(MachineInstructionParameterNode machineInstructionParameterNode);
}
