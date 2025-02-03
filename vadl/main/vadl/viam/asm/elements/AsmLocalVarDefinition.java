package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents a local variable definition within a grammar rule.
 */
public class AsmLocalVarDefinition implements AsmGrammarElement {
  String localVarName;
  @Nullable
  AsmGrammarElement asmLiteral;

  public AsmLocalVarDefinition(String localVarName, @Nullable AsmGrammarElement asmLiteral) {
    this.localVarName = localVarName;
    this.asmLiteral = asmLiteral;
  }
}
