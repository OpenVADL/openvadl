package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents the usage of a local variable in a grammar rule.
 */
public class AsmLocalVarUse implements AsmGrammarElement {
  @Nullable
  AsmAssignTo assignToElement;
  String invokedLocalVar;

  public AsmLocalVarUse(@Nullable AsmAssignTo assignToElement, String invokedLocalVar) {
    this.assignToElement = assignToElement;
    this.invokedLocalVar = invokedLocalVar;
  }
}
