package vadl.viam;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * An Instruction Set Architecture (ISA) definition of a VADL specification.
 */
public class InstructionSetArchitecture extends Definition {

  // this ISA is an extension of the dependency
  @Nullable
  private final InstructionSetArchitecture superIsaRef;

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


  private final Specification specification;

  /**
   * Constructs an InstructionSetArchitecture object with the given parameters.
   *
   * @param identifier    the identifier of the ISA
   * @param specification the parent specification of the ISA
   * @param superIsaRef   the ISA this ISA is extending (might be null)
   * @param registers     the registers in the ISA. This also includes sub-registers
   * @param registerFiles the register files in the ISA
   * @param pc            the program counter of the ISA
   * @param formats       the list of formats associated with the ISA
   * @param instructions  the list of instructions associated with the ISA
   */
  public InstructionSetArchitecture(Identifier identifier,
                                    Specification specification,
                                    @Nullable
                                    InstructionSetArchitecture superIsaRef,
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
    this.superIsaRef = superIsaRef;
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
  public InstructionSetArchitecture dependencyRef() {
    return superIsaRef;
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
    if (pc != null) {
      return pc;
    }
    if (superIsaRef != null) {
      return superIsaRef.pc();
    }
    return null;
  }

  /**
   * Returns a stream of {@link Instruction}s that are available in the scope of this ISA.
   * This includes definitions in the super ISA as well as the instructions specified in
   * this ISA.
   */
  public Stream<Instruction> scopedInstructions() {
    return Streams.concat(
        this.instructions.stream(),
        superIsaRef != null ? superIsaRef.scopedInstructions() : Stream.of()
    );
  }


  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
