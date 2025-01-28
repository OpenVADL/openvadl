package vadl.viam.asm.rules;

import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;

/**
 * A TerminalRule is a simple rule referencing just a single token of the lexical analysis.
 */
public class TerminalRule extends GrammarRule {

  String value;

  public TerminalRule(Identifier identifier, String value) {
    super(identifier);
    this.value = value;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
