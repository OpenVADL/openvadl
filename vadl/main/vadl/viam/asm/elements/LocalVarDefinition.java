package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents a local variable definition within a grammar rule.
 */
public class LocalVarDefinition implements GrammarElement {
  String localVarName;
  @Nullable
  GrammarElement asmLiteral;

  public LocalVarDefinition(String localVarName, @Nullable GrammarElement asmLiteral) {
    this.localVarName = localVarName;
    this.asmLiteral = asmLiteral;
  }
}
