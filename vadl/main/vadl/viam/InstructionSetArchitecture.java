package vadl.viam;

import java.util.List;

/**
 * An Instruction Set Architecture (ISA) definition of a VADL specification.
 */
public class InstructionSetArchitecture extends Definition {

  private final List<Instruction> instructions;

  public InstructionSetArchitecture(Identifier identifier, List<Instruction> instructions) {
    super(identifier);
    this.instructions = instructions;
  }

  public List<Instruction> instructions() {
    return instructions;
  }
}
