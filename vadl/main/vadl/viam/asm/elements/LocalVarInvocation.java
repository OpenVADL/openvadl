package vadl.viam.asm.elements;

import javax.annotation.Nullable;

/**
 * Represents the usage of a local variable in a grammar rule.
 */
public class LocalVarInvocation implements GrammarElement {
  @Nullable
  String attributeOrLocalVar;
  String invokedLocalVar;

  public LocalVarInvocation(@Nullable String attributeOrLocalVar, String invokedLocalVar) {
    this.attributeOrLocalVar = attributeOrLocalVar;
    this.invokedLocalVar = invokedLocalVar;
  }
}
