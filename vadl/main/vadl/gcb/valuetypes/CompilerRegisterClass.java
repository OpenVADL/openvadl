package vadl.gcb.valuetypes;

import java.util.List;
import vadl.viam.Abi;
import vadl.viam.RegisterFile;

/**
 * Extends the concept of the register class for the compiler.
 */
public class CompilerRegisterClass {
  private final String name;
  private final RegisterFile registerFile;
  private final List<CompilerRegister> registers;
  private final Abi.Alignment alignment;

  /**
   * Constructor.
   */
  public CompilerRegisterClass(RegisterFile registerFile,
                               List<CompilerRegister> registers,
                               Abi.Alignment alignment) {
    this.name = registerFile.identifier.simpleName();
    this.registerFile = registerFile;
    this.registers = registers;
    this.alignment = alignment;
  }

  public String name() {
    return name;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  public List<CompilerRegister> registers() {
    return registers;
  }

  public Abi.Alignment alignment() {
    return alignment;
  }
}
