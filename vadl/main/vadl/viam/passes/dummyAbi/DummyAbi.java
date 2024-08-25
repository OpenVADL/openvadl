package vadl.viam.passes.dummyAbi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.RegisterFile;

/**
 * Proof of concept ABI for RISC-V until we have a solution.
 */
public class DummyAbi extends Definition {

  enum Alignment {
    NO_ALIGNMENT(-1),
    HALF_WORD(4),
    WORD(8);

    private final int byteAlignment;

    Alignment(int byteAlignment) {
      this.byteAlignment = byteAlignment;
    }
  }

  public record RegisterRef(RegisterFile registerFile,
                     int addr,
                     Alignment alignment) {
    public String render() {
      return registerFile.identifier.simpleName() + addr;
    }
  }

  record RegisterAlias(String value) {
  }



  private final RegisterRef returnAddress;
  private final RegisterRef stackPointer;
  private final RegisterRef framePointer;


  private final Map<RegisterRef, RegisterAlias> aliases;
  private final List<RegisterRef> callerSaved;
  private final List<RegisterRef> calleeSaved;
  private final List<RegisterRef> argumentRegisters;
  private final List<RegisterRef> returnRegisters;

  /**
   * Constructor.
   */
  public DummyAbi(Identifier identifier,
                  RegisterRef returnAddress,
                  RegisterRef stackPointer,
                  RegisterRef framePointer,
                  Map<RegisterRef, RegisterAlias> aliases,
                  List<RegisterRef> callerSaved,
                  List<RegisterRef> calleeSaved,
                  List<RegisterRef> argumentRegisters,
                  List<RegisterRef> returnRegisters
  ) {
    super(identifier);
    this.returnAddress = returnAddress;
    this.stackPointer = stackPointer;
    this.framePointer = framePointer;
    this.aliases = aliases;
    this.callerSaved = callerSaved;
    this.calleeSaved = calleeSaved;
    this.argumentRegisters = argumentRegisters;
    this.returnRegisters = returnRegisters;
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

  public Map<RegisterRef, RegisterAlias> aliases() {
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
}
