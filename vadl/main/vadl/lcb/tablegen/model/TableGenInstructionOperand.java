package vadl.lcb.tablegen.model;

/**
 * An {@link TableGenInstruction} has list of operands for inputs and outputs.
 * This class represent one element of the inputs or outputs.
 */
public record TableGenInstructionOperand(String type, String name) {
}
