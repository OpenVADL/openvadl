package vadl.viam;

import javax.annotation.Nullable;

/**
 * Represents VADLs Micro Processor definition.
 * It is used by the ISS and LCB and defines a combination of ISA and ABI together with
 * additional information like the emulation start address, emulation stop condition,
 * default firmware, and startup functionality.
 */
public class MicroProcessor extends Definition {

  private final InstructionSetArchitecture isa;

  private final Abi abi;


  private final Function start;

  @Nullable
  private final Function stop;

  /**
   * Constructs the microprocessor.
   */
  public MicroProcessor(Identifier identifier, InstructionSetArchitecture isa, Abi abi,
                        Function start, @Nullable Function stop) {
    super(identifier);
    this.isa = isa;
    this.abi = abi;
    this.start = start;
    this.stop = stop;
  }

  public Abi abi() {
    return abi;
  }

  public InstructionSetArchitecture isa() {
    return isa;
  }

  public Function start() {
    return start;
  }

  @Nullable
  public Function stop() {
    return stop;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
