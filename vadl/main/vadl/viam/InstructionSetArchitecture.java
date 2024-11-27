package vadl.viam;

import java.util.List;
import javax.annotation.Nullable;

/**
 * An Instruction Set Architecture (ISA) definition of a VADL specification.
 */
public class InstructionSetArchitecture extends Definition {

  private final List<Instruction> instructions;
  private final List<PseudoInstruction> pseudoInstructions;

  private final List<Register> registers;
  private final List<RegisterFile> registerFiles;
  private final List<Memory> memories;

  @Nullable
  private final Counter pc;

  private final List<Format> formats;
  private final List<Function> functions;
  private final List<Relocation> relocations;


  @SuppressWarnings("unused")
  private final Specification specification;

  /**
   * Constructs an InstructionSetArchitecture object with the given parameters.
   *
   * @param identifier    the identifier of the ISA
   * @param specification the parent specification of the ISA
   * @param registers     the registers in the ISA. This also includes sub-registers
   * @param registerFiles the register files in the ISA
   * @param pc            the program counter of the ISA
   * @param formats       the list of formats associated with the ISA
   * @param instructions  the list of instructions associated with the ISA
   */
  public InstructionSetArchitecture(Identifier identifier,
                                    Specification specification,
                                    List<Format> formats,
                                    List<Function> functions,
                                    List<Relocation> relocations,
                                    List<Instruction> instructions,
                                    List<PseudoInstruction> pseudoInstructions,
                                    List<Register> registers,
                                    List<RegisterFile> registerFiles,
                                    @Nullable Counter pc,
                                    List<Memory> memories
  ) {
    super(identifier);
    this.specification = specification;
    this.formats = formats;
    this.functions = functions;
    this.relocations = relocations;
    this.registers = registers;
    this.instructions = instructions;
    this.pseudoInstructions = pseudoInstructions;
    this.registerFiles = registerFiles;
    this.pc = pc;
    this.memories = memories;

    // set parent architecture of instructions
    for (var instr : instructions) {
      instr.setParentArchitecture(this);
    }
  }

  @Nullable
  // TODO: Remove
  public InstructionSetArchitecture dependencyRef() {
    return null;
  }

  /**
   * Returns the {@link Instruction}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<Instruction> ownInstructions() {
    return instructions;
  }

  /**
   * Returns the {@link Function}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<Function> ownFunctions() {
    return functions;
  }

  /**
   * Returns the {@link Relocation}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<Relocation> ownRelocations() {
    return relocations;
  }

  /**
   * Returns the {@link PseudoInstruction}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<PseudoInstruction> ownPseudoInstructions() {
    return pseudoInstructions;
  }

  /**
   * Returns the {@link Register}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<Register> ownRegisters() {
    return registers;
  }

  /**
   * Returns the {@link RegisterFile}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<RegisterFile> ownRegisterFiles() {
    return registerFiles;
  }

  /**
   * Returns the {@link Format}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<Format> ownFormats() {
    return formats;
  }

  /**
   * Returns the {@link Memory}s <b>owned</b> by this ISA.
   * So it might not include definitions accessible through the super ISA.
   */
  public List<Memory> ownMemories() {
    return memories;
  }

  /**
   * Returns the program counter used by this ISA.
   * If the definition was in the super ISA, it will use that one instead.
   */
  @Nullable
  public Counter pc() {
    return pc;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
