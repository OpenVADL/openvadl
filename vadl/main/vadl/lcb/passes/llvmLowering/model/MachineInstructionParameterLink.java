package vadl.lcb.passes.llvmLowering.model;

import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Sometimes it is useful to replace arguments of {@link MachineInstructionNode}.
 * However, the {@link MachineInstructionNode} does not know what the {@code address}
 * or {@code value} is. This is container structure which contains a link the
 * selection dag node which the machine instruction's parameter references.
 * So we can easily identify the links between an input operand  and a machine pattern.
 */
public record MachineInstructionParameterLink(TableGenInstructionOperand inputOperand,
                                              ExpressionNode machine) {
}
