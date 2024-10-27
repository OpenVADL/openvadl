package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.PseudoInstructionNode;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for machine instruction's {@link Graph}.
 */
public interface TableGenMachineInstructionVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link PseudoInstructionNode}.
   */
  void visit(PseudoInstructionNode pseudoInstructionNode);

  /**
   * Visit {@link MachineInstructionNode}.
   */
  void visit(MachineInstructionNode machineInstructionNode);

  /**
   * Visit {@link MachineInstructionParameterNode}.
   */
  void visit(MachineInstructionParameterNode machineInstructionParameterNode);

  /**
   * Visit {@link MachineInstructionValueNode}.
   */
  void visit(MachineInstructionValueNode machineInstructionValueNode);
}
