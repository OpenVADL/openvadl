package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents the usage of a string literal in a grammar rule.
 */
public class AsmStringLiteralUse implements AsmGrammarElement {
  @Nullable
  AsmAssignTo assignToElement;
  String value;

  public AsmStringLiteralUse(@Nullable AsmAssignTo assignToElement, String value) {
    this.assignToElement = assignToElement;
    this.value = value;
  }
}
