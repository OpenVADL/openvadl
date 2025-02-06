package vadl.viam.asm.elements;

import java.util.List;
import vadl.viam.asm.AsmGrammarVisitor;

/**
 * Represents a choice of different alternatives in a grammar rule.
 * It contains a list of single alternatives.
 */
public record AsmAlternatives(List<AsmAlternative> alternatives) implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
