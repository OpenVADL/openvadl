package vadl.lcb.passes.llvmLowering.strategies.visitors;

import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionValueNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBasicBlockSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFieldAccessRefNode;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphNodeVisitor;

/**
 * Visitor for machine instruction's {@link Graph}.
 */
public interface TableGenMachineInstructionVisitor extends GraphNodeVisitor {
  /**
   * Visit {@link LcbPseudoInstructionNode}.
   */
  void visit(LcbPseudoInstructionNode pseudoInstructionNode);

  /**
   * Visit {@link LcbMachineInstructionNode}.
   */
  void visit(LcbMachineInstructionNode machineInstructionNode);

  /**
   * Visit {@link LcbMachineInstructionParameterNode}.
   */
  void visit(LcbMachineInstructionParameterNode machineInstructionParameterNode);

  /**
   * Visit {@link LcbMachineInstructionValueNode}.
   */
  void visit(LcbMachineInstructionValueNode machineInstructionValueNode);

  /**
   * Visit {@link LlvmBasicBlockSD}.
   */
  void visit(LlvmBasicBlockSD basicBlockSD);

  /**
   * Visit {@link LlvmFieldAccessRefNode}.
   */
  void visit(LlvmFieldAccessRefNode fieldAccessRefNode);
}
