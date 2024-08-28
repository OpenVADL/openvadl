package vadl.viam;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * An Instruction Set Architecture (ISA) definition of a VADL specification.
 */
public class InstructionSetArchitecture extends Definition {

  // this ISA is an extension of the dependency
  @Nullable
  private final InstructionSetArchitecture dependencyRef;

  private final List<Instruction> instructions;
  private final List<PseudoInstruction> pseudoInstructions;

  private final List<Register> registers;
  private final List<RegisterFile> registerFiles;

  // The pc of the ISA. This field does not "own" the register
  // but only references it. The pc register is "owned" by the `registers` field.
  @Nullable
  private final Register.Counter pc;

  private final List<Format> formats;
  private final List<Function> functions;
  private final List<Relocation> relocations;
  private final List<Memory> memories;


  private final Specification specification;

  /**
   * Constructs an InstructionSetArchitecture object with the given parameters.
   *
   * @param identifier    the identifier of the ISA
   * @param specification the parent specification of the ISA
   * @param dependencyRef the ISA this ISA is extending (might be null)
   * @param registers     the registers in the ISA. This also includes sub-registers
   * @param registerFiles the register files in the ISA
   * @param pc            the program counter of the ISA
   * @param formats       the list of formats associated with the ISA
   * @param instructions  the list of instructions associated with the ISA
   */
  public InstructionSetArchitecture(Identifier identifier,
                                    Specification specification,
                                    @Nullable
                                    InstructionSetArchitecture dependencyRef,
                                    List<Format> formats,
                                    List<Function> functions,
                                    List<Relocation> relocations,
                                    List<Instruction> instructions,
                                    List<PseudoInstruction> pseudoInstructions,
                                    List<Register> registers,
                                    List<RegisterFile> registerFiles,
                                    @Nullable Register.Counter pc,
                                    List<Memory> memories
  ) {
    super(identifier);
    this.specification = specification;
    this.dependencyRef = dependencyRef;
    this.formats = formats;
    this.functions = functions;
    this.relocations = relocations;
    this.registers = registers;
    this.instructions = instructions;
    this.pseudoInstructions = pseudoInstructions;
    this.registerFiles = registerFiles;
    this.pc = pc;
    this.memories = memories;
  }

  @Nullable
  public InstructionSetArchitecture dependencyRef() {
    return dependencyRef;
  }

  public List<Instruction> instructions() {
    return instructions;
  }

  public List<Function> functions() {
    return functions;
  }

  public List<Relocation> relocations() {
    return relocations;
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


  @Nullable
  public Register.Counter pc() {
    return pc;
  }

  /**
   * Get all group counters in this ISA.
   */
  public List<Register.Counter> groupCounters() {
    return registers().stream()
        .filter(Register.Counter.class::isInstance)
        .map(Register.Counter.class::cast)
        .filter(e -> e != pc)
        .toList();
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
