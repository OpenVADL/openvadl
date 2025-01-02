package vadl.vdt.utils;

/**
 * Represents an instruction in the decode tree.
 *
 * <p>In addition to the underlying source instruction from the VIAM,
 * an instruction is augmented with relevant information for decoding, such as the width of the
 * instruction and the fixed bit pattern that represents the instruction.
 */
public record Instruction(vadl.viam.Instruction source, int width, BitPattern pattern) {
}
