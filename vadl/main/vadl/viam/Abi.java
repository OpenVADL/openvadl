package vadl.viam;

import java.util.List;
import java.util.Map;
import vadl.utils.Pair;

/**
 * VADL ABI representation.
 */
public class Abi extends Definition {

  /**
   * Register Spilling Alignments.
   */
  public enum Alignment {
    NO_ALIGNMENT(-1),
    HALF_WORD(4),
    WORD(8);

    @SuppressWarnings("unused")
    private final int byteAlignment;

    Alignment(int byteAlignment) {
      this.byteAlignment = byteAlignment;
    }
  }

  /**
   * Constructor.
   *
   * @param registerFile is the "parent" of the register.
   * @param addr         represents the index in a register file.
   *                     E.g., RISC-V's X11 would have {@code addr = 11}.
   * @param alignment    for the spilling of the register.
   */
  public record RegisterRef(RegisterFile registerFile,
                            int addr,
                            Alignment alignment) {
    public String render() {
      return registerFile.identifier.simpleName() + addr;
    }
  }

  /**
   * Value type for alias.
   */
  public record RegisterAlias(String value) {
  }


  private final RegisterRef returnAddress;
  private final RegisterRef stackPointer;
  private final RegisterRef globalPointer;
  private final RegisterRef framePointer;
  private final RegisterRef threadPointer;


  private final Map<Pair<RegisterFile, Integer>, RegisterAlias> aliases;
  private final List<RegisterRef> callerSaved;
  private final List<RegisterRef> calleeSaved;
  private final List<RegisterRef> argumentRegisters;
  private final List<RegisterRef> returnRegisters;
  private final PseudoInstruction returnSequence;
  private final PseudoInstruction callSequence;
  private final PseudoInstruction addressSequence;

  /**
   * Constructor.
   */
  public Abi(Identifier identifier,
             RegisterRef returnAddress,
             RegisterRef stackPointer,
             RegisterRef framePointer,
             RegisterRef globalPointer,
             RegisterRef threadPointer,
             Map<Pair<RegisterFile, Integer>, RegisterAlias> aliases,
             List<RegisterRef> callerSaved,
             List<RegisterRef> calleeSaved,
             List<RegisterRef> argumentRegisters,
             List<RegisterRef> returnRegisters,
             PseudoInstruction returnSequence,
             PseudoInstruction callSequence,
             PseudoInstruction addressSequence
  ) {
    super(identifier);
    this.returnAddress = returnAddress;
    this.stackPointer = stackPointer;
    this.framePointer = framePointer;
    this.globalPointer = globalPointer;
    this.threadPointer = threadPointer;
    this.aliases = aliases;
    this.callerSaved = callerSaved;
    this.calleeSaved = calleeSaved;
    this.argumentRegisters = argumentRegisters;
    this.returnRegisters = returnRegisters;
    this.returnSequence = returnSequence;
    this.callSequence = callSequence;
    this.addressSequence = addressSequence;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }


  public RegisterRef returnAddress() {
    return returnAddress;
  }

  public RegisterRef stackPointer() {
    return stackPointer;
  }

  public RegisterRef framePointer() {
    return framePointer;
  }

  public RegisterRef globalPointer() {
    return globalPointer;
  }

  public RegisterRef threadPointer() {
    return threadPointer;
  }

  public Map<Pair<RegisterFile, Integer>, RegisterAlias> aliases() {
    return aliases;
  }

  public List<RegisterRef> callerSaved() {
    return callerSaved;
  }

  public List<RegisterRef> calleeSaved() {
    return calleeSaved;
  }

  public List<RegisterRef> argumentRegisters() {
    return argumentRegisters;
  }

  public List<RegisterRef> returnRegisters() {
    return returnRegisters;
  }

  public boolean hasFramePointer() {
    return true;
  }

  public PseudoInstruction returnSequence() {
    return returnSequence;
  }

  public PseudoInstruction callSequence() {
    return callSequence;
  }

  public PseudoInstruction addressSequence() {
    return addressSequence;
  }
}
