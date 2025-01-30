package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.graph.Graph;

/**
 * Represents a single alternative in a grammar rule.
 * Contains a list of grammar elements and an optional semantic predicate.
 */
public class AsmAlternative implements AsmGrammarElement {
  @Nullable
  Graph semanticPredicate;
  List<AsmGrammarElement> elements;

  public AsmAlternative(@Nullable Graph semanticPredicate, List<AsmGrammarElement> elements) {
    this.semanticPredicate = semanticPredicate;
    this.elements = elements;
  }
}
