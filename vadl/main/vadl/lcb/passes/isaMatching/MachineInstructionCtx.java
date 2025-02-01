package vadl.lcb.passes.isaMatching;

import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;

/**
 * An extension for the {@link Instruction}. It will be used
 * label the instruction with a {@link MachineInstructionLabel}.
 */
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
