package vadl.viam;

import java.util.List;
import java.util.stream.Stream;

/**
 * An Instruction Set Architecture (ISA) definition of a VADL specification.
 */
public class InstructionSetArchitecture extends Definition {

  private final List<Instruction> instructions;
  private final List<PseudoInstruction> pseudoInstructions;
  private final List<Register> registers;
  private final List<RegisterFile> registerFiles;
  private final List<Format> formats;
  private final List<Memory> memories;
  private final Specification specification;

  /**
   * Constructs an InstructionSetArchitecture object with the given parameters.
   *
   * @param identifier    the identifier of the ISA
   * @param specification the parent specification of the ISA
   * @param registers     the registers in the ISA. This also includes sub-registers
   * @param registerFiles the register files in the ISA
   * @param formats       the list of formats associated with the ISA
   * @param instructions  the list of instructions associated with the ISA
   */
  public InstructionSetArchitecture(Identifier identifier,
                                    Specification specification,
                                    List<Format> formats,
                                    List<Instruction> instructions,
                                    List<PseudoInstruction> pseudoInstructions,
                                    List<Register> registers,
                                    List<RegisterFile> registerFiles,
                                    List<Memory> memories
  ) {
    super(identifier);
    this.specification = specification;
    this.formats = formats;
    this.registers = registers;
    this.instructions = instructions;
    this.pseudoInstructions = pseudoInstructions;
    this.registerFiles = registerFiles;
    this.memories = memories;
  }

  public List<Instruction> instructions() {
    return instructions;
  }

  public List<PseudoInstruction> pseudoInstructions() {
    return pseudoInstructions;
  }

  public List<Register> registers() {
    return registers;
  }

  public List<RegisterFile> registerFiles() {
    return registerFiles;
  }

  /**
   * Returns the formats defined in this ISA.
   */
  public List<Format> formats() {
    return formats;
  }

  /**
   * Returns a stream of all formats available in this ISA. This includes all formats of the
   * outer specification scope.
   */
  public Stream<Format> availableFormats() {
    return Stream.concat(formats.stream(), specification.formats());
  }

  public List<Memory> memories() {
    return memories;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
