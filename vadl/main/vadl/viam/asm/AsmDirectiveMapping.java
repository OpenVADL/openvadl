package vadl.viam.asm;

import vadl.ast.AsmDirective;
import vadl.utils.SourceLocation;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;

/**
 * Introduces a new name in the assembly language for a directive.
 */
public class AsmDirectiveMapping extends Definition {
  private final String alias;
  private final AsmDirective directive;

  /**
   * Creates a new directive mapping.
   *
   * <p>
   * Sets the identifier of the definition superclass,
   * the related AsmDirective and the source location.
   * </p>
   *
   * @param identifier the identifier of the directive
   * @param directive  the directive to be mapped to
   * @param location   the source location of the directive mapping
   */
  public AsmDirectiveMapping(Identifier identifier, String alias,
                             AsmDirective directive, SourceLocation location) {
    super(identifier);
    this.alias = alias;
    this.directive = directive;
    setSourceLocation(location);
  }

  public String getAlias() {
    return alias;
  }

  public AsmDirective getDirective() {
    return directive;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
