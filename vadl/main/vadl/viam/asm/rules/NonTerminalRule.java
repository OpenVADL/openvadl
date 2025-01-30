package vadl.viam.asm.rules;

import vadl.utils.SourceLocation;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.asm.elements.Alternatives;

/**
 * A NonTerminalRule is a complex rule composed of references to other rules,
 * calls to functions, literals and block elements like Group, Option, Repetition.
 */
public class NonTerminalRule extends GrammarRule {

  Alternatives alternatives;

  /**
   * Creates a new NonTerminalRule.
   *
   * @param identifier   the identifier of the rule
   * @param alternatives the alternatives of the rule
   * @param location     the source location of the rule
   */
  public NonTerminalRule(Identifier identifier, Alternatives alternatives,
                         SourceLocation location) {
    super(identifier);
    this.alternatives = alternatives;
    this.setSourceLocation(location);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
