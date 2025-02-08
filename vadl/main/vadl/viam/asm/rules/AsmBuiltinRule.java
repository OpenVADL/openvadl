package vadl.viam.asm.rules;

import vadl.types.asmTypes.AsmType;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * A rule for which a parsing function already exists in LLVM.
 */
public class AsmBuiltinRule extends AsmGrammarRule {
  public AsmBuiltinRule(Identifier identifier, AsmType asmType) {
    super(identifier, asmType);
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
