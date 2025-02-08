package vadl.viam.asm.rules;

import vadl.types.asmTypes.AsmType;
import vadl.viam.Definition;
import vadl.viam.Identifier;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * GrammarRules define the grammar of the assembly language,
 * from which the assembly parser is generated.
 */
public abstract class AsmGrammarRule extends Definition {

  private final AsmType asmType;

  public AsmGrammarRule(Identifier identifier, AsmType asmType) {
    super(identifier);
    this.asmType = asmType;
  }

  public AsmType getAsmType() {
    return asmType;
  }

  public abstract void accept(AsmGrammarVisitor visitor);
}