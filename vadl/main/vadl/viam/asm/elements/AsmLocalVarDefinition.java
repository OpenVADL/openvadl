package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * Represents a local variable definition within a grammar rule.
 */
public record AsmLocalVarDefinition(String localVarName,
                                    @Nullable AsmGrammarElement asmLiteral, AsmType asmType)
    implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
