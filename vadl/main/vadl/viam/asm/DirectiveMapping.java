package vadl.viam.asm;

import vadl.ast.AsmDirective;
import vadl.utils.SourceLocation;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;

/**
 * Introduces a new name in the assembly language for a directive.
 */
public class DirectiveMapping extends Definition {
  AsmDirective directive;

  public DirectiveMapping(Identifier identifier, AsmDirective directive, SourceLocation location) {
    super(identifier);
    this.directive = directive;
    setSourceLocation(location);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
