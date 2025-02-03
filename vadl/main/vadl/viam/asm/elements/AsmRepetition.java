package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.viam.graph.Graph;

/**
 * Represents a repetition block ({@code {}}) in a grammar rule.
 */
public class AsmRepetition implements AsmGrammarElement {
  @Nullable
  Graph semanticPredicate;
  AsmAlternatives alternatives;

  public AsmRepetition(@Nullable Graph semanticPredicate, AsmAlternatives alternatives) {
    this.semanticPredicate = semanticPredicate;
    this.alternatives = alternatives;
  }
}
