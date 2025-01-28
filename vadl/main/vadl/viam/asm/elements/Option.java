package vadl.viam.asm.elements;

import javax.annotation.Nullable;
import vadl.viam.graph.Graph;

/**
 * Represents an optional block ({@code []}) in a grammar rule.
 */
public class Option implements GrammarElement {
  @Nullable
  Graph semanticPredicate;
  Alternatives alternatives;

  public Option(@Nullable Graph semanticPredicate, Alternatives alternatives) {
    this.semanticPredicate = semanticPredicate;
    this.alternatives = alternatives;
  }
}
