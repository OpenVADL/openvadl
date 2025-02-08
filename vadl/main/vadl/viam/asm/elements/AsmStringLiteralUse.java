package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * Represents the usage of a string literal in a grammar rule.
 */
public record AsmStringLiteralUse(@Nullable AsmAssignTo assignToElement,
                                  String value, AsmType asmType) implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
