package vadl.lcb.passes.llvmLowering.visitors;

import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
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

  /**
   * Visit {@link LlvmBrCcSD}.
   */
  void visit(LlvmBrCcSD node);

  /**
   * Visit {@link LlvmFieldAccessRefNode}.
   */
  void visit(LlvmFieldAccessRefNode llvmFieldAccessRefNode);
}
