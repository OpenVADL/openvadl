package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.viam.graph.Graph;

/**
 * Represents an optional block ({@code []}) in a grammar rule.
 */
public class AsmOption implements AsmGrammarElement {
  @Nullable
  Graph semanticPredicate;
  AsmAlternatives alternatives;

  public AsmOption(@Nullable Graph semanticPredicate, AsmAlternatives alternatives) {
    this.semanticPredicate = semanticPredicate;
    this.alternatives = alternatives;
  }
}
