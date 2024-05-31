package vadl.viam;

import java.util.List;
import java.util.stream.Stream;

/**
 * An Instruction Set Architecture (ISA) definition of a VADL specification.
 */
public class InstructionSetArchitecture extends Definition {

  private final List<Instruction> instructions;
  private final List<PseudoInstruction> pseudoInstructions;
  private final List<Format> formats;
  private final Specification specification;

  /**
   * Constructs an InstructionSetArchitecture object with the given parameters.
   *
   * @param identifier    the identifier of the InstructionSetArchitecture
   * @param specification the parent specification of the InstructionSetArchitecture
   * @param formats       the list of formats associated with the InstructionSetArchitecture
   * @param instructions  the list of instructions associated with the InstructionSetArchitecture
   */
  public InstructionSetArchitecture(Identifier identifier,
                                    Specification specification,
                                    List<Format> formats,
                                    List<Instruction> instructions,
                                    List<PseudoInstruction> pseudoInstructions) {
    super(identifier);
    this.specification = specification;
    this.formats = formats;
    this.instructions = instructions;
    this.pseudoInstructions = pseudoInstructions;
  }

  public List<Instruction> instructions() {
    return instructions;
  }

  public List<PseudoInstruction> pseudoInstructions() {
    return pseudoInstructions;
  }

  public Stream<Format> formats() {
    return Stream.concat(formats.stream(), specification.formats());
  }


  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
