package vadl.vdt.model;

import java.util.Collection;
import vadl.vdt.utils.Instruction;

/**
 * Generates a decode decision tree from a collection of instructions.
 *
 * @param <T> Depending on the generation algorithm, the type of instruction may be limited to a
 *            specific type.
 */
public interface DecodeTreeGenerator<T extends Instruction> {

  Node generate(Collection<T> instructions);

}
