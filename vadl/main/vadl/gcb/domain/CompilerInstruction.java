package vadl.gcb.domain;

import vadl.viam.PseudoInstruction;

/**
 * A compiler instruction is like a {@link PseudoInstruction} but
 * it does not automatically outputted into tablegen and there is no assembly for it.
 */
public class CompilerInstruction extends PseudoInstruction {
  /**
   * Instantiates a CompilerInstruction object and verifies it.
   */
  public CompilerInstruction(PseudoInstruction pseudoInstruction) {
    super(pseudoInstruction.identifier,
        pseudoInstruction.parameters(),
        pseudoInstruction.behavior(),
        pseudoInstruction.assembly());
  }
}
