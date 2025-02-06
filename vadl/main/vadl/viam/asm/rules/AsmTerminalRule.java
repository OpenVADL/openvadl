package vadl.viam.asm.rules;

import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * A TerminalRule is a simple rule referencing just a single token of the lexical analysis.
 */
public class AsmTerminalRule extends AsmGrammarRule {

  private final String value;

  public AsmTerminalRule(Identifier identifier, String value) {
    super(identifier);
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
