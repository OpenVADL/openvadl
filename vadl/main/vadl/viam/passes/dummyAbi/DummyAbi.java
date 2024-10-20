package vadl.viam.passes.dummyAbi;

import java.util.List;
import java.util.Map;
import vadl.utils.Pair;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.PseudoInstruction;
import vadl.viam.RegisterFile;

/**
 * Proof of concept ABI for RISC-V until we have a solution.
 */
public class DummyAbi extends Definition {

  enum Alignment {
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
   * @param addr         represents the index in a register file. X11 would have {@code addr = 11}.
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


  private final Map<Pair<RegisterFile, Integer>, RegisterAlias> aliases;
  private final List<RegisterRef> callerSaved;
  private final List<RegisterRef> calleeSaved;
  private final List<RegisterRef> argumentRegisters;
  private final List<RegisterRef> returnRegisters;
  private final PseudoInstruction returnSequence;

  /**
   * Constructor.
   */
  public DummyAbi(Identifier identifier,
                  RegisterRef returnAddress,
                  RegisterRef stackPointer,
                  RegisterRef framePointer,
                  RegisterRef globalPointer,
                  Map<Pair<RegisterFile, Integer>, RegisterAlias> aliases,
                  List<RegisterRef> callerSaved,
                  List<RegisterRef> calleeSaved,
                  List<RegisterRef> argumentRegisters,
                  List<RegisterRef> returnRegisters,
                  PseudoInstruction returnSequence
  ) {
    super(identifier);
    this.returnAddress = returnAddress;
    this.stackPointer = stackPointer;
    this.framePointer = framePointer;
    this.globalPointer = globalPointer;
    this.aliases = aliases;
    this.callerSaved = callerSaved;
    this.calleeSaved = calleeSaved;
    this.argumentRegisters = argumentRegisters;
    this.returnRegisters = returnRegisters;
    this.returnSequence = returnSequence;
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
}
