package vadl.lcb.passes.isaMatching;

import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Relocation;

/**
 * An extension for the {@link Relocation}. It will be used
 * label the instruction with a {@link RelocationFunctionLabel}.
 */
public class RelocationCtx extends DefinitionExtension<Relocation> {
  private final RelocationFunctionLabel label;

  public RelocationCtx(RelocationFunctionLabel label) {
    this.label = label;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public RelocationFunctionLabel label() {
    return label;
  }
}
