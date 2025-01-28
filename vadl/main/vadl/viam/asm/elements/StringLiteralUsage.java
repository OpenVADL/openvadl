package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents the usage of a string literal in a grammar rule.
 */
public class StringLiteralUsage implements GrammarElement {
  @Nullable
  String attributeOrLocalVar;
  String value;

  public StringLiteralUsage(@Nullable String attributeOrLocalVar, String value) {
    this.attributeOrLocalVar = attributeOrLocalVar;
    this.value = value;
  }
}
