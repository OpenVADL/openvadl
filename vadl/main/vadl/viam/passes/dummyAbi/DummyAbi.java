package vadl.viam.passes.dummyAbi;

import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.RegisterFile;

/**
 * Proof of concept ABI for RISC-V until we have a solution.
 */
public class DummyAbi extends Definition {

  enum Alignment {
    HALF_WORD(4),
    WORD(8);

    private final int byteAlignment;

    Alignment(int byteAlignment) {
      this.byteAlignment = byteAlignment;
    }
  }

  record RegisterRef(RegisterFile registerFile,
                     int addr,
                     Alignment alignment) {

  }

  private final RegisterRef returnAddress;
  private final RegisterRef stackPointer;
  private final RegisterRef framePointer;

  /**
   * Constructor.
   */
  public DummyAbi(Identifier identifier,
                  RegisterRef returnAddress,
                  RegisterRef stackPointer,
                  RegisterRef framePointer
  ) {
    super(identifier);
    this.returnAddress = returnAddress;
    this.stackPointer = stackPointer;
    this.framePointer = framePointer;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
