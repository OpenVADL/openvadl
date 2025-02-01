package vadl.lcb.passes.isaMatching;

import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;

public class MachineInstructionCtx extends DefinitionExtension<Instruction> {
  private final MachineInstructionLabel label;

  public MachineInstructionCtx(MachineInstructionLabel label) {
    this.label = label;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public MachineInstructionLabel label() {
    return label;
  }
}
