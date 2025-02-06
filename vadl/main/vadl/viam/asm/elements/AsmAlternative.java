package vadl.viam.asm.elements;

import java.util.List;
import javax.annotation.Nullable;
import vadl.viam.asm.AsmGrammarVisitor;
import vadl.viam.graph.Graph;

/**
 * Represents a single alternative in a grammar rule.
 * Contains a list of grammar elements and an optional semantic predicate.
 */
public record AsmAlternative(@Nullable Graph semanticPredicate,
                             List<AsmGrammarElement> elements) implements AsmGrammarElement {

  @Override
  public void accept(AsmGrammarVisitor visitor) {
    visitor.visit(this);
  }
}
