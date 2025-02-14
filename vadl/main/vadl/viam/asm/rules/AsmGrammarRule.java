package vadl.viam.asm.rules;

import vadl.types.asmTypes.AsmType;
import vadl.viam.Definition;
import vadl.viam.Identifier;
import vadl.viam.asm.elements.AsmGrammarElement;

/**
 * GrammarRules define the grammar of the assembly language,
 * from which the assembly parser is generated.
 */
public abstract class AsmGrammarRule extends Definition implements AsmGrammarElement {

  private final AsmType asmType;

  public AsmGrammarRule(Identifier identifier, AsmType asmType) {
    super(identifier);
    this.asmType = asmType;
  }

  public AsmType getAsmType() {
    return asmType;
  }
}