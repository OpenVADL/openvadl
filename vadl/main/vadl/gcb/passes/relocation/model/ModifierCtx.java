package vadl.gcb.passes.relocation.model;

import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Relocation;

/**
 * Context for the {@link Modifier}.
 */
public class ModifierCtx extends DefinitionExtension<Relocation> {
  private final Modifier absoluteModifier;
  private final Modifier relativeModifier;

  public ModifierCtx(Modifier absoluteModifier, Modifier relativeModifier) {
    this.absoluteModifier = absoluteModifier;
    this.relativeModifier = relativeModifier;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public Modifier absoluteModifier() {
    return absoluteModifier;
  }

  public Modifier relativeModifier() {
    return relativeModifier;
  }
}
