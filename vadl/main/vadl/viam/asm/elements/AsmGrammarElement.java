package vadl.viam.asm.elements;

import vadl.viam.asm.AsmGrammarVisitor;

/**
 * Represents a building block for grammar rules.
 * A grammar rule is a composition of grammar elements.
 */
public interface AsmGrammarElement {
  void accept(AsmGrammarVisitor visitor);
}
