package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.viam.graph.Graph;

/**
 * Represents a repetition block ({@code {}}) in a grammar rule.
 */
public class Repetition implements GrammarElement {
  @Nullable
  Graph semanticPredicate;
  Alternatives alternatives;

  public Repetition(@Nullable Graph semanticPredicate, Alternatives alternatives) {
    this.semanticPredicate = semanticPredicate;
    this.alternatives = alternatives;
  }
}
