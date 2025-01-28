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
public class Modifier extends Definition {
  Relocation relocation;

  public Modifier(Identifier identifier, Relocation relocation, SourceLocation location) {
    super(identifier);
    this.relocation = relocation;
    setSourceLocation(location);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
