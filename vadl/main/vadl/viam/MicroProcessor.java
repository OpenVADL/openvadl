package vadl.viam;

import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Represents VADLs Micro Processor definition.
 * It is used by the ISS and LCB and defines a combination of ISA and ABI together with
 * additional information like the emulation start address, emulation stop condition,
 * default firmware, and startup functionality.
 */
public class MicroProcessor extends Definition {

  private final String targetName;

  private final InstructionSetArchitecture isa;

  @Nullable
  private final Abi abi;

  @Nullable
  private final Function start;

  @Nullable
  private final Function stop;

  /**
   * Constructs the microprocessor.
   */
  public MicroProcessor(Identifier identifier, InstructionSetArchitecture isa, @Nullable Abi abi,
                        @Nullable Function start, @Nullable Function stop,
                        @Nullable String targetName) {
    super(identifier);
    this.isa = isa;
    this.abi = abi;
    this.start = start;
    this.stop = stop;
    if (targetName != null) {
      this.targetName = targetName;
    } else {
      this.targetName = isa.simpleName();
    }
  }

  /**
   * Returns the abi and throws if the abi isn't set.
   *
   * @return the abi.
   */
  public Abi abi() {
    if (abi == null) {
      throw new IllegalStateException("abi must be set");
    }
    return abi;
  }

  public @Nullable Abi abiNullable() {
    return abi;
  }

  public InstructionSetArchitecture isa() {
    return isa;
  }

  /**
   * Returns the start and throws if it isn't set.
   *
   * @return the start.
   */
  public Function start() {
    if (start == null) {
      throw new IllegalStateException("start must be set");
    }
    return start;
  }

  public @Nullable Function startNullable() {
    return start;
  }

  @Nullable
  public Function stop() {
    return stop;
  }

  public Stream<Instruction> instructions() {
    return isa.ownInstructions().stream();
  }

  public String targetName() {
    return targetName;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
