package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents the usage of a string literal in a grammar rule.
 */
public class StringLiteralUse implements GrammarElement {
  @Nullable
  AssignTo assignToElement;
  String value;

  public StringLiteralUse(@Nullable AssignTo assignToElement, String value) {
    this.assignToElement = assignToElement;
    this.value = value;
  }
}
