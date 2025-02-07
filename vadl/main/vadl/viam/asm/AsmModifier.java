package vadl.viam.asm;

import vadl.utils.SourceLocation;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * Defines a name to be used to reference a relocation
 * of the instruction set architecture in the assembly language.
 */
public class AsmModifier extends Definition {
  private final Relocation relocation;

  /**
   * Create a new asm modifier.
   * <p>
   * Sets the identifier of the definition superclass,
   * the related relocation and the source location.
   * </p>
   *
   * @param identifier the name of the modifier
   * @param relocation the relocation to be used
   * @param location   the source location of the modifier
   */
  public AsmModifier(Identifier identifier, Relocation relocation, SourceLocation location) {
    super(identifier);
    this.relocation = relocation;
    setSourceLocation(location);
  }

  public Relocation getRelocation() {
    return relocation;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
