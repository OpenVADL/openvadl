package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * Represents the usage of a local variable in a grammar rule.
 */
public record AsmLocalVarUse(@Nullable AsmAssignTo assignToElement,
                             String invokedLocalVar) implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
