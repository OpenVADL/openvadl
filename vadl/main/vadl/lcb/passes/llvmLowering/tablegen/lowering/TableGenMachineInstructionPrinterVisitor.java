package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenMachineInstructionVisitor;

/**
 * Visitor for machine instructions.
 */
public class TableGenMachineInstructionPrinterVisitor extends TableGenPatternPrinterVisitor
    implements TableGenMachineInstructionVisitor {
  @Override
  public void visit(MachineInstructionNode node) {
    writer.write("(");
    writer.write(node.instruction().identifier.simpleName() + " ");

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }
}
