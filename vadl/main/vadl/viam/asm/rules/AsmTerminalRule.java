package vadl.viam.asm.rules;

import vadl.types.asmTypes.AsmType;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;

/**
 * A TerminalRule is a simple rule referencing just a single token of the lexical analysis.
 */
public class AsmTerminalRule extends AsmGrammarRule {

  private final String value;

  public AsmTerminalRule(Identifier identifier, String value, AsmType asmType) {
    super(identifier, asmType);
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
