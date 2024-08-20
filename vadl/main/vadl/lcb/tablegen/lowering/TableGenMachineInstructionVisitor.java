package vadl.lcb.tablegen.lowering;

import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.visitors.TableGenMachineInstructionLcbVisitor;

/**
 * Visitor for machine instructions.
 */
public class TableGenMachineInstructionVisitor extends TableGenPatternVisitor
    implements TableGenMachineInstructionLcbVisitor {
  @Override
  public void visit(MachineInstructionNode node) {
    writer.write("(");
    writer.write(node.instruction().identifier.simpleName() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }
}
