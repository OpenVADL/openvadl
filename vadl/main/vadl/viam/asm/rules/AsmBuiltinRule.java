package vadl.viam.asm.rules;

import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;

/**
 * A rule for which a parsing function already exists in LLVM.
 */
public class AsmBuiltinRule extends AsmGrammarRule {
  public AsmBuiltinRule(Identifier identifier) {
    super(identifier);
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
