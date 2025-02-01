package vadl.lcb.passes.isaMatching;

import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
* An extension for the {@link PseudoInstruction}. It will be used
* label the instruction with a {@link PseudoInstructionLabel}.
*/
public class PseudoInstructionCtx extends DefinitionExtension<Instruction> {
  private final PseudoInstructionLabel label;

  public PseudoInstructionCtx(PseudoInstructionLabel label) {
    this.label = label;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public PseudoInstructionLabel label() {
    return label;
  }
}
