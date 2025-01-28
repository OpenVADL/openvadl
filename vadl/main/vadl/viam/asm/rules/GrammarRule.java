package vadl.viam.asm.rules;

import vadl.viam.Definition;
import vadl.viam.Identifier;

/**
 * GrammarRules define the grammar of the assembly language,
 * from which the assembly parser is generated.
 */
public abstract class GrammarRule extends Definition {

  public GrammarRule(Identifier identifier) {
    super(identifier);
  }
}