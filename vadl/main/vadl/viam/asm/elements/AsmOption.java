package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.viam.asm.AsmGrammarVisitor;
import vadl.viam.graph.Graph;

/**
 * Represents an optional block ({@code []}) in a grammar rule.
 */
public record AsmOption(@Nullable Graph semanticPredicate,
                        AsmAlternatives alternatives) implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
