package vadl.viam.asm.rules;

import vadl.viam.Definition;
import vadl.viam.Identifier;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * GrammarRules define the grammar of the assembly language,
 * from which the assembly parser is generated.
 */
public abstract class AsmGrammarRule extends Definition {

  public AsmGrammarRule(Identifier identifier) {
    super(identifier);
  }

  abstract void accept(AsmGrammarVisitor visitor);
}