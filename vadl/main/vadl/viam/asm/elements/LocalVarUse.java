package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents the usage of a local variable in a grammar rule.
 */
public class LocalVarUse implements GrammarElement {
  @Nullable
  AssignTo assignToElement;
  String invokedLocalVar;

  public LocalVarUse(@Nullable AssignTo assignToElement, String invokedLocalVar) {
    this.assignToElement = assignToElement;
    this.invokedLocalVar = invokedLocalVar;
  }
}
