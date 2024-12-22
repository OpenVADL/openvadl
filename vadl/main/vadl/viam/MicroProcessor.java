package vadl.viam;

import javax.annotation.Nullable;

public class MicroProcessor extends Definition {

  private final InstructionSetArchitecture isa;

  private final Abi abi;


  private final Function start;

  @Nullable
  private final Function stop;

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
